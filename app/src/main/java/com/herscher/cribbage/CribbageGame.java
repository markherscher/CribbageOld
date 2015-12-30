package com.herscher.cribbage;

import android.support.annotation.NonNull;

import com.herscher.cribbage.scoring.PlayScoreProcessor;
import com.herscher.cribbage.scoring.ShowdownScoreProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO COMMENT
 * TODO Create interface
 * <p/>
 * This class is not thread-safe. The thread used for public methods will be used to fire events.
 */
public class CribbageGame
{
	public enum State
	{
		NEW,
		DISCARD,
		PLAY,
		SCORE,
		COMPLETED
	}

	public interface ActionHandler
	{
		void discardCards(@NonNull Player player, @NonNull Card[] cards) throws RulesViolationException;
		ScoreUnit[] playCard(@NonNull Player player, @NonNull Card card) throws RulesViolationException;
	}

	private final static int PLAY_COUNT = 4;
	private final static int DISCARD_COUNT = 2;
	private final static int DEAL_COUNT = PLAY_COUNT + DISCARD_COUNT;
	private final static int POINTS_TO_WIN = 100;// TODO: made up

	private final List<Listener> listeners;
	private final List<Player> players;
	private final List<Card> crib;
	private final PlayScoreProcessor playScoreProcessor;
	private final ShowdownScoreProcessor showdownScoreProcessor;
	private CardCollection allCards;
	private State state;
	private Player dealer;
	private Player activePlayer;
	private Player winner;
	private Card cutCard;
	private ActionHandler currentActionHandler;

	public CribbageGame(@NonNull PlayScoreProcessor playScoreProcessor, @NonNull ShowdownScoreProcessor showdownScoreProcessor, @NonNull Player[] players)
	{
		if (players == null || players.length <= 1)
		{
			throw new IllegalArgumentException();
		}

		this.playScoreProcessor = playScoreProcessor;
		this.showdownScoreProcessor = showdownScoreProcessor;
		this.players = new ArrayList<>();
		listeners = new CopyOnWriteArrayList<>();
		crib = new ArrayList<>();
		state = State.NEW;

		for (Player p : players)
		{
			if (p == null)
			{
				throw new IllegalArgumentException();
			}

			this.players.add(p);
		}
	}

	public void addListener(@NonNull Listener l)
	{
		if (l == null)
		{
			throw new IllegalArgumentException();
		}

		synchronized (listeners)
		{
			if (!listeners.contains(l))
			{
				listeners.add(l);
			}
		}
	}

	public void removeListener(Listener l)
	{
		synchronized (listeners)
		{
			listeners.remove(l);
		}
	}

	/**
	 * Must be already shuffled.
	 *
	 * @param allCards
	 */
	public void startGame(@NonNull CardCollection allCards)
	{
		if (state != State.NEW)
		{
			throw new IllegalStateException();
		}

		if (allCards.remaining() < getMinimumDealCount())
		{
			throw new IllegalArgumentException(String
					.format("Required at least %d cards to deal, but only have %d", allCards
							.remaining(), getMinimumDealCount()));
		}

		state = State.SCORE;
		startNewRound(allCards);
	}

	/**
	 * Must be already shuffled.
	 *
	 * @param allCards
	 */
	public void startNewRound(@NonNull CardCollection allCards)
	{
		if (state != State.SCORE)
		{
			throw new IllegalStateException("state is not SCORE");
		}

		if (allCards.remaining() < getMinimumDealCount())
		{
			throw new IllegalArgumentException(String
					.format("Required at least %d cards to deal, but only have %d", allCards
							.remaining(), getMinimumDealCount()));
		}

		if (!checkForWinner())
		{
			for (Player p : players)
			{
				p.getDiscardedCards().clear();
			}

			this.allCards = new CardCollection(allCards);
			allCards.reset();
			dealer = getNextPlayer(dealer);
			activePlayer = null;
			cutCard = null;
			state = State.DISCARD;
			playScoreProcessor.reset();

			dealCards();

			for (Listener l : listeners)
			{
				l.onRoundStarted();
				l.onDiscardRequired(DISCARD_COUNT);
			}
		}
	}

	public void discardCards(@NonNull Player player, @NonNull Card[] cards) throws RulesViolationException
	{
		currentActionHandler.discardCards(player, cards);

		for (Listener l : listeners)
		{
			l.onCardsDiscarded(player, cards);
		}

		// If everyone has discarded then we can start playing
		if (allPlayersDiscarded())
		{
			startPlayState();
		}
	}

	public void playCard(@NonNull Player player, @NonNull Card card) throws RulesViolationException
	{
		ScoreUnit[] scoreUnits = currentActionHandler.playCard(player, card);

		for (Listener l : listeners)
		{
			l.onCardPlayed(activePlayer, card);
		}

		for (ScoreUnit su : scoreUnits)
		{
			int pointChange = su.getPoints();

			if (pointChange != 0)
			{
				activePlayer.addScore(su.getPoints());

				for (Listener l : listeners)
				{
					l.onScoreChanged(activePlayer, su);
				}
			}
		}

		if (!checkForWinner())
		{
			continuePlay();
		}
	}

	/**
	 * Gets the next player that can legally play, or null if no one can. Remember this for the
	 * play only, not lead.
	 *
	 * @return
	 */
	private Player getNextLegalPlayerForPlay()
	{
		// Find a player that can play a card.
		Player testPlayer = activePlayer;

		do
		{
			testPlayer = getNextPlayer(testPlayer);

			for (Card c : testPlayer.getHand())
			{
				if (isCardLegalToPlay(c, testPlayer))
				{
					return testPlayer;
				}
			}
		} while (testPlayer != activePlayer);

		return null;
	}

	private Player getNextLegalPlayerForLead()
	{
		Player testPlayer = activePlayer;

		do
		{
			testPlayer = getNextPlayer(testPlayer);

			if (testPlayer.getHand().size() > 0)
			{
				return testPlayer;
			}
		} while (testPlayer != activePlayer);

		return null;
	}

	private void continuePlay()
	{
		Player playPlayer = getNextLegalPlayerForPlay();
		Player leadPlayer = getNextLegalPlayerForLead();

		if (playPlayer != null)
		{
			// Someone can play a card
			setActivePlayer(playPlayer);

			for (Listener l : listeners)
			{
				l.onPlayRequired(activePlayer);
			}
		}
		else if (leadPlayer != null)
		{
			// No one can play a card, but if we reset the count someone can play
			playScoreProcessor.reset();
			setActivePlayer(leadPlayer);

			for (Listener l : listeners)
			{
				l.onLeadRequired(activePlayer);
			}
		}
		else
		{
			// No one can play or lead, so round is over
			// No one available to lead, so round is over
			// TODO: need to score here
			state = State.SCORE;

			// TODO: score in correct order. First winner wins.
			//showdownScoreProcessor.calculateScore()

			for (Listener l : listeners)
			{
				l.onRoundCompleted();
			}
		}
	}

	private void scoreShowdown()
	{

	}

	private void scoreCardPlayed()
	{

	}

	private void finishGame(Player winner)
	{
		state = State.COMPLETED;
		this.winner = winner;

		for (Listener l : listeners)
		{
			l.onGameCompleted(winner);
		}
	}

	private int getMinimumDealCount()
	{
		return (players.size() * DEAL_COUNT) + 1; // +1 for cut card
	}

	public boolean isCardLegalToPlay(Card card, Player player)
	{
		return player.getHand().contains(card) && playScoreProcessor.isCardLegalToPlay(card);
	}

	public State getState()
	{
		return state;
	}

	public Player[] getAllPlayers()
	{
		return players.toArray(new Player[players.size()]);
	}

	public Player getWinner()
	{
		return winner;
	}

	public Card getCutCard()
	{
		return cutCard;
	}

	public int getPlayCount()
	{
		return playScoreProcessor.getCount();
	}

	private boolean checkForWinner()
	{
		if (winner == null)
		{
			for (Player p : players)
			{
				if (p.getCurrentScore() >= POINTS_TO_WIN)
				{
					finishGame(p);
				}
			}

			// No winner
			return false;
		}
		else
		{
			// Someone already won
			return true;
		}
	}

	private void startPlayState()
	{
		setActivePlayer(getNextPlayer(dealer));
		state = State.PLAY;

		for (Listener l : listeners)
		{
			l.onPlayStarted(cutCard);
			l.onLeadRequired(activePlayer);
		}
	}

	private void setActivePlayer(Player p)
	{
		activePlayer = p;
	}

	private void dealCards()
	{
		int index = 0;
		crib.clear();
		cutCard = allCards.getNext();

		for (Player p : players)
		{
			List<Card> hand = p.getHand();
			hand.clear();

			for (int i = 0; i < DEAL_COUNT; i++)
			{
				hand.add(allCards.getNext());
			}
		}
	}

	private boolean allPlayersDiscarded()
	{
		for (Player p : players)
		{
			if (p.getDiscardedCards().size() == 0)
			{
				return false;
			}
		}

		return true;
	}

	private Player getNextPlayer(Player p)
	{
		if (p == null)
		{
			return players.get(0);
		}
		else
		{
			int index = players.indexOf(p) + 1;
			if (index >= players.size())
			{
				index = 0;
			}

			return players.get(index);
		}
	}

	public interface Listener
	{
		void onRoundStarted();

		void onPlayStarted(Card cutCard);

		void onDiscardRequired(int cardCount);

		void onPlayRequired(Player player);

		void onLeadRequired(Player player);

		void onCardsDiscarded(Player player, Card[] cards);

		void onCardPlayed(Player player, Card card);

		void onScoreChanged(Player player, ScoreUnit reason);

		void onRoundCompleted();

		void onGameCompleted(Player winningPlayer);
	}
}
