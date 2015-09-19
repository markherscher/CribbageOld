package com.herscher.cribbage;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO COMMENT
 * TODO Create interface
 */
public class CribbageGame
{
	public enum State
	{
		DISCARD,
		PLAY,
		SCORE,
		COMPLETED
	}

	private final static int PLAYER_DEAL_COUNT = 6;
	private final static int DISCARD_COUNT = 2;
	private final static int POINTS_TO_WIN = 100;// TODO: made up

	private final List<Listener> listeners;
	private final List<Player> players;
	private final List<Card> crib;
	private final StandardPlayScoringProcessor playScoreProcessor;
	private CardCollection allCards;
	private State state;
	private Player dealer;
	private Player activePlayer;
	private Player winner;
	private Card cutCard;

	public CribbageGame(@NonNull StandardPlayScoringProcessor playScoreProcessor, @NonNull Player[] players)
	{
		if (players == null || players.length <= 1)
		{
			throw new IllegalArgumentException();
		}

		this.playScoreProcessor = playScoreProcessor;
		this.players = new ArrayList<>();
		listeners = new CopyOnWriteArrayList<>();
		crib = new ArrayList<>();

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
	 * @param allCards
	 */
	public void startGame(@NonNull CardCollection allCards)
	{
		if (state != State.COMPLETED)
		{
			throw new IllegalStateException();
		}

		startNewRound(allCards);
	}

	// TODO: verify there are enough cards

	/**
	 * Must be already shuffled.
	 * @param allCards
	 */
	public void startNewRound(@NonNull CardCollection allCards)
	{
		if (!checkForWinner())
		{
			for (Player p : players)
			{
				p.getDiscardedCards().clear();
			}

			this.allCards = new CardCollection(allCards);
			dealer = getNextPlayer(dealer);
			activePlayer = null;
			cutCard = null;
			state = State.DISCARD;

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
		if (player == null || cards == null)
		{
			throw new IllegalArgumentException();
		}

		List<Card> playerHand = player.getHand();

		for (Card c : cards)
		{
			if (c == null)
			{
				throw new IllegalArgumentException();
			}

			if (!playerHand.contains(c))
			{
				throw new RulesViolationException("card is not in player's hand");
			}
		}

		if (state != State.DISCARD)
		{
			throw new RulesViolationException("state is not DISCARD");
		}

		if (cards.length != DISCARD_COUNT)
		{
			throw new RulesViolationException("incorrect number of cards discarded");
		}

		if (players.indexOf(player) < 0)
		{
			throw new RulesViolationException("player is not in the game");
		}

		List<Card> playerDiscard = player.getDiscardedCards();

		if (playerDiscard.size() != 0)
		{
			throw new RulesViolationException("player has already discarded");
		}

		for (Card c : cards)
		{
			playerDiscard.add(c);
			crib.add(c);
		}

		for (Listener l : listeners)
		{
			l.onCardsDiscarded(player, cards);
		}

		if (allPlayersDiscarded())
		{
			startPlayState();
		}
	}

	public void playCard(@NonNull Player player, @NonNull Card card) throws RulesViolationException
	{
		if (player == null || card == null)
		{
			throw new IllegalArgumentException();
		}

		if (state != State.PLAY)
		{
			throw new RulesViolationException("state is not PLAY");
		}

		if (!player.equals(activePlayer))
		{
			throw new RulesViolationException("player is not the active player");
		}

		if (!player.getHand().contains(card))
		{
			throw new RulesViolationException("card is not in player's hand");
		}

		if (!isCardLegalToPlay(card))
		{
			throw new RulesViolationException("card is not legal to play");
		}

		// TODO: Score all at end of round
		ScoreUnit scoreUnits[] = playScoreProcessor.playCard(card);

		for (Listener l : listeners)
		{
			l.onCardPlayed(activePlayer, card);
		}

		for (ScoreUnit su : scoreUnits)
		{
			activePlayer.addScore(su.getPoints());

			for (Listener l : listeners)
			{
				l.onScoreChanged(activePlayer);
			}
		}

		if (!checkForWinner())
		{
			continuePlay();
		}
	}

	private void continuePlay()
	{
		Player localActivePlayer = activePlayer;
		boolean cardsRemain = false;

		// Check that there are cards remaining to play. If not the round is over.
		Player testPlayer = localActivePlayer;

		do
		{
			testPlayer = getNextPlayer(testPlayer);

			if (testPlayer.getHand().size() > 0)
			{
				cardsRemain = true;
				break;
			}
		} while (testPlayer != activePlayer);

		if (cardsRemain)
		{
			// Find a player that can play a card. If there are none then the count must be reset.
			boolean isLegalCard = false;
			testPlayer = localActivePlayer;
			do
			{
				for (Card c : testPlayer.getHand())
				{
					if (playScoreProcessor.isCardLegalToPlay(c))
					{
						isLegalCard = true;
						break;
					}
				}
			} while (testPlayer != activePlayer);

			if (testPlayer == null)
			{
				// No valid cards can be played, so reset the count
				// TODO: must give one point to active player
				playScoreProcessor.reset();
				setActivePlayer(getNextPlayer(activePlayer));
			}
			else
			{
				setActivePlayer(testPlayer);
			}

			for (Listener l : listeners)
			{
				l.onPlayRequired(activePlayer);
			}
		}
		else
		{
			// Round is over
			for (Listener l : listeners)
			{
				l.onRoundCompleted();
			}
		}
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

	public boolean isCardLegalToPlay(Card card)
	{
		return playScoreProcessor.isCardLegalToPlay(card);
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
			l.onPlayRequired(activePlayer);
		}
	}

	private void setActivePlayer(Player p)
	{
		activePlayer = p;
		playScoreProcessor.setPlayer(activePlayer);
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

			for (int i = 0; i < PLAYER_DEAL_COUNT; i++)
			{
				hand.add(allCards.getNext());
			}
		}
	}

	private boolean allPlayersDiscarded()
	{
		for (Player p : players)
		{
			if (p.getDiscardedCards().size() != 0)
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

		void onCardsDiscarded(Player player, Card[] cards);

		void onCardPlayed(Player player, Card card);

		void onScoreChanged(Player player);

		void onRoundCompleted();

		void onGameCompleted(Player winningPlayer);
	}
}
