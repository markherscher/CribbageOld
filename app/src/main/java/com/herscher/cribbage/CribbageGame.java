package com.herscher.cribbage;

import android.support.annotation.NonNull;

import com.herscher.cribbage.scoring.PlayScoreProcessor;
import com.herscher.cribbage.scoring.ShowdownScoreProcessor;

import java.util.ArrayList;
import java.util.List;

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

	private final static int PLAY_COUNT = 4;
	private final static int DISCARD_COUNT = 2;
	private final static int DEAL_COUNT = PLAY_COUNT + DISCARD_COUNT;
	private final static int POINTS_TO_WIN = 100;// TODO: made up

	private final List<Player> players;
	private final List<Card> crib;
	private final PlayScoreProcessor playScoreProcessor;
	private final ShowdownScoreProcessor showdownScoreProcessor;
	private final NewRoundStateActionHandler newRoundStateActionHandler;
	private final DiscardStateActionHandler discardStateActionHandler;
	private final PlayStateActionHandler playStateActionHandler;
	private CardCollection allCards;
	private Player dealer;
	private Player activePlayer;
	private Player winner;
	private Card cutCard;
	private StateActionHandler stateActionHandler;

	public CribbageGame(@NonNull PlayScoreProcessor playScoreProcessor, @NonNull ShowdownScoreProcessor showdownScoreProcessor, @NonNull Player[] players)
	{
		if (players == null || players.length <= 1)
		{
			throw new IllegalArgumentException();
		}

		this.playScoreProcessor = playScoreProcessor;
		this.showdownScoreProcessor = showdownScoreProcessor;
		this.players = new ArrayList<>();
		crib = new ArrayList<>();
		newRoundStateActionHandler = new NewRoundStateActionHandler();
		discardStateActionHandler = new DiscardStateActionHandler();
		playStateActionHandler = new PlayStateActionHandler();

		for (Player p : players)
		{
			if (p == null)
			{
				throw new IllegalArgumentException();
			}

			this.players.add(p);
		}

		setStateActionHandler(new NewGameStateActionHandler());
	}

	/**
	 * Must be already shuffled.
	 *
	 * @param allCards
	 */
	public void startGame(@NonNull CardCollection allCards) throws RulesViolationException
	{
		stateActionHandler.startGame(allCards);
	}

	/**
	 * Must be already shuffled.
	 *
	 * @param allCards
	 */
	public void startNewRound(@NonNull CardCollection allCards) throws RulesViolationException
	{
		stateActionHandler.startGame(allCards);
	}

	public void discardCards(@NonNull Player player, @NonNull Card... cards) throws RulesViolationException
	{
		stateActionHandler.discardCards(player, cards);
	}

	public ScoreUnit[] playCard(@NonNull Player player, @NonNull Card card) throws RulesViolationException
	{
		return stateActionHandler.playCard(player, card);
	}

	public int getMinimumDealCount()
	{
		return (players.size() * DEAL_COUNT) + 1; // +1 for cut card
	}

	public boolean isCardLegalToPlay(Card card, Player player)
	{
		return player.getHand().contains(card) && playScoreProcessor.isCardLegalToPlay(card);
	}

	public Card[] getCrib()
	{
		return crib.toArray(new Card[crib.size()]);
	}

	public State getState()
	{
		return stateActionHandler.getState();
	}

	public Player[] getAllPlayers()
	{
		return players.toArray(new Player[players.size()]);
	}

	public Player getActivePlayer()
	{
		return activePlayer;
	}

	public Player getDealingPlayer()
	{
		return dealer;
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
					// TODO need to do more
					winner = p;

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

	private void setActivePlayer(Player p)
	{
		activePlayer = p;
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

	private abstract class StateActionHandler
	{
		private final State state;

		public StateActionHandler(State state)
		{
			this.state = state;
		}

		public State getState()
		{
			return state;
		}

		public void startGame(@NonNull CardCollection cards) throws RulesViolationException
		{
			throw new RulesViolationException(String.format("cannot start game during state %s", state));
		}

		public void startNewRound(@NonNull CardCollection cards) throws RulesViolationException
		{
			throw new RulesViolationException(String.format("cannot start round during state %s", state));
		}

		public void discardCards(@NonNull Player player, @NonNull Card[] cards) throws RulesViolationException
		{
			throw new RulesViolationException(String.format("cannot discard cards during state %s", state));
		}

		public ScoreUnit[] playCard(@NonNull Player player, @NonNull Card card) throws RulesViolationException
		{
			throw new RulesViolationException(String.format("cannot play card during state %s", state));
		}
	}

	private class NewGameStateActionHandler extends StateActionHandler
	{
		public NewGameStateActionHandler()
		{
			super(State.NEW);
		}

		@Override
		public void startGame(@NonNull CardCollection allCards) throws RulesViolationException
		{
			if (allCards.remaining() < getMinimumDealCount())
			{
				throw new RulesViolationException(String
						.format("Required at least %d cards to deal, but only have %d", allCards
								.remaining(), getMinimumDealCount()));
			}

			setStateActionHandler(newRoundStateActionHandler);
			stateActionHandler.startNewRound(allCards);
		}
	}

	private class NewRoundStateActionHandler extends StateActionHandler
	{
		public NewRoundStateActionHandler()
		{
			// Re-use the NEW state because this state transitions immediately to the DISCARD state
			// anyway
			super(State.NEW);
		}

		@Override
		public void startNewRound(@NonNull CardCollection allCards) throws RulesViolationException
		{
			if (allCards.remaining() < getMinimumDealCount())
			{
				throw new RulesViolationException(String
						.format("Required at least %d cards to deal, but only have %d", allCards
								.remaining(), getMinimumDealCount()));
			}

			if (!checkForWinner())
			{
				for (Player p : players)
				{
					p.getDiscardedCards().clear();
				}

				CribbageGame.this.allCards = new CardCollection(allCards);
				dealer = getNextPlayer(dealer);
				activePlayer = null;
				cutCard = null;
				playScoreProcessor.reset();

				dealCards();
				setStateActionHandler(discardStateActionHandler);
			}
		}

		private void dealCards()
		{
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
	}

	private class DiscardStateActionHandler extends StateActionHandler
	{
		public DiscardStateActionHandler()
		{
			super(State.DISCARD);
		}

		@Override
		public void discardCards(@NonNull Player player, @NonNull Card[] cards) throws RulesViolationException
		{
			validateInputs(player, cards);

			List<Card> playerHand = player.getHand();
			List<Card> playerDiscard = player.getDiscardedCards();

			for (Card c : cards)
			{
				playerHand.remove(c);
				playerDiscard.add(c);
				crib.add(c);
			}

			// If everyone has discarded then we can start playing
			if (allPlayersDiscarded())
			{
				setActivePlayer(getNextPlayer(dealer));
				setStateActionHandler(playStateActionHandler);
			}
		}

		private void validateInputs(Player player, Card[] cards) throws RulesViolationException
		{
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
	}

	private class PlayStateActionHandler extends StateActionHandler
	{
		public PlayStateActionHandler()
		{
			super(State.PLAY);
		}

		@Override
		public ScoreUnit[] playCard(@NonNull Player player, @NonNull Card card) throws RulesViolationException
		{
			List<Card> playerHand = player.getHand();
			List<Card> playerPlayedCards = player.getPlayedCards();

			if (!player.equals(activePlayer))
			{
				throw new RulesViolationException("player is not the active player");
			}

			if (!isCardLegalToPlay(card, activePlayer))
			{
				throw new RulesViolationException("card is not legal to play");
			}

			playerHand.remove(card);
			playerPlayedCards.add(card);

			ScoreUnit[] scoreUnits = playScoreProcessor.playCard(card);

			for (ScoreUnit su : scoreUnits)
			{
				int pointChange = su.getPoints();

				if (pointChange != 0)
				{
					activePlayer.addScore(su.getPoints());
				}
			}

			if (!checkForWinner())
			{
				continuePlay();
			}

			// TODO: Score all at end of round
			return scoreUnits;
		}

		private void continuePlay()
		{
			Player playPlayer = getNextLegalPlayerForPlay();
			Player leadPlayer = getNextLegalPlayerForLead();

			if (playPlayer != null)
			{
				// Someone can play a card
				setActivePlayer(playPlayer);
			}
			else if (leadPlayer != null)
			{
				// No one can play a card, but if we reset the count someone can play
				playScoreProcessor.reset();
				setActivePlayer(leadPlayer);
			}
			else
			{
				// No one can play or lead, so round is over
				// No one available to lead, so round is over
				// TODO: need to score here

				// TODO: score in correct order. First winner wins.
				//showdownScoreProcessor.calculateScore()
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

		public boolean isCardLegalToPlay(Card card, Player player)
		{
			return player.getHand().contains(card) && playScoreProcessor.isCardLegalToPlay(card);
		}
	}

	private void setStateActionHandler(StateActionHandler handler)
	{
		if (handler == null)
		{
			throw new IllegalArgumentException();
		}

		stateActionHandler = handler;
	}

	/*
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
	*/
}
