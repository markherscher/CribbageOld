package com.herscher.cribbage;

import android.support.annotation.NonNull;

import com.herscher.cribbage.scoring.PlayScoreProcessor;
import com.herscher.cribbage.scoring.ScoreUnit;
import com.herscher.cribbage.scoring.ShowdownScoreProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
		ROUND_COMPLETE,
		GAME_COMPLETE
	}

	// TODO; PROVIDE THESE SOMEHOW? Public functions rely on these.
	private final static int PLAY_COUNT = 4;
	private final static int DISCARD_COUNT = 2;
	private final static int DEAL_COUNT = PLAY_COUNT + DISCARD_COUNT;
	private final static int POINTS_TO_WIN = 100;// TODO: made up

	private final List<PlayerState> players;
	private final List<Card> crib;
	private final PlayScoreProcessor playScoreProcessor;
	private final ShowdownScoreProcessor showdownScoreProcessor;
	private final NewRoundStateActionHandler newRoundStateActionHandler;
	private final DiscardStateActionHandler discardStateActionHandler;
	private final PlayStateActionHandler playStateActionHandler;
	private final long randomSeed;
	private final long gameId;
	private CardCollection allCards;
	private PlayerState dealer;
	private PlayerState activePlayer;
	private PlayerState winner;
	private Card cutCard;
	private StateActionHandler stateActionHandler;

	public CribbageGame(@NonNull PlayScoreProcessor playScoreProcessor, @NonNull ShowdownScoreProcessor showdownScoreProcessor, int playerCount)
	{
		if (playScoreProcessor == null || showdownScoreProcessor == null || playerCount <= 1)
		{
			throw new IllegalArgumentException();
		}

		Random random = new Random();

		this.playScoreProcessor = playScoreProcessor;
		this.showdownScoreProcessor = showdownScoreProcessor;
		this.players = new ArrayList<>();
		crib = new ArrayList<>();
		newRoundStateActionHandler = new NewRoundStateActionHandler();
		discardStateActionHandler = new DiscardStateActionHandler();
		playStateActionHandler = new PlayStateActionHandler();
		randomSeed = random.nextLong();
		gameId = random.nextLong();

		for (int i = 0; i < playerCount; i++)
		{
			players.add(new PlayerState(i));
		}

		setStateActionHandler(new NewGameStateActionHandler());
	}

	/**
	 * Must be already shuffled.
	 *
	 * @param allCards
	 */
	public void startGame(CardCollection allCards) throws RulesViolationException
	{
		stateActionHandler.startGame(allCards);
	}

	public void startNewRound() throws RulesViolationException
	{
		stateActionHandler.startNewRound(allCards);
	}

	public void discardCards(@NonNull PlayerState player, @NonNull Card... cards) throws RulesViolationException
	{
		stateActionHandler.discardCards(player, cards);
	}

	public ScoreUnit[] playCard(@NonNull PlayerState player, @NonNull Card card) throws RulesViolationException
	{
		return stateActionHandler.playCard(player, card);
	}

	public int getMinimumDealCount()
	{
		return (players.size() * DEAL_COUNT) + 1; // +1 for cut card
	}

	public boolean isCardLegalToPlay(Card card, PlayerState player)
	{
		return player.getHand().contains(card) && playScoreProcessor.isCardLegalToPlay(card);
	}

	public ScoreUnit[] getEndOfRoundScoringForPlayer(PlayerState player)
	{
		// TODO
		return new ScoreUnit[0];
	}

	public Card[] getCrib()
	{
		return crib.toArray(new Card[crib.size()]);
	}

	public State getState()
	{
		return stateActionHandler.getState();
	}

	public PlayerState[] getAllPlayers()
	{
		return players.toArray(new PlayerState[players.size()]);
	}

	public PlayerState getActivePlayer()
	{
		return activePlayer;
	}

	public PlayerState getDealingPlayer()
	{
		return dealer;
	}

	public PlayerState getWinner()
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

	public int getCardsPerDiscard()
	{
		return DISCARD_COUNT;
	}

	public long getGameId()
	{
		return gameId;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof CribbageGame)
		{
			return gameId == ((CribbageGame)o).gameId;
		}
		else
		{
			return false;
		}
	}

	private boolean checkForWinner()
	{
		if (winner == null)
		{
			for (PlayerState p : players)
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

	private void setActivePlayer(PlayerState p)
	{
		activePlayer = p;
	}

	private PlayerState getNextPlayer(PlayerState p)
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

	private static abstract class StateActionHandler
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

		public void discardCards(@NonNull PlayerState player, @NonNull Card[] cards) throws RulesViolationException
		{
			throw new RulesViolationException(String.format("cannot discard cards during state %s", state));
		}

		public ScoreUnit[] playCard(@NonNull PlayerState player, @NonNull Card card) throws RulesViolationException
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

	private static class NewRoundStateActionHandler extends StateActionHandler
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
				for (PlayerState p : players)
				{
					p.getDiscardedCards().clear();
				}

				allCards.shuffle(randomSeed);
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

			for (PlayerState p : players)
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
		public void discardCards(@NonNull PlayerState player, @NonNull Card[] cards) throws RulesViolationException
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

		private void validateInputs(PlayerState player, Card[] cards) throws RulesViolationException
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
			for (PlayerState p : players)
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
		public ScoreUnit[] playCard(@NonNull PlayerState player, @NonNull Card card) throws RulesViolationException
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

			// TODO need to go to end of game state
			return scoreUnits;
		}

		private void continuePlay()
		{
			PlayerState playPlayer = getNextLegalPlayerForPlay();
			PlayerState leadPlayer = getNextLegalPlayerForLead();

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
				// TODO: score in correct order. First winner wins.
				//showdownScoreProcessor.calculateScore()

				// Actually, probably should switch to a new state to handle scoring at round end
			}
		}

		/**
		 * Gets the next player that can legally play, or null if no one can. Remember this for the
		 * play only, not lead.
		 *
		 * @return
		 */
		private PlayerState getNextLegalPlayerForPlay()
		{
			// Find a player that can play a card.
			PlayerState testPlayer = activePlayer;

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

		private PlayerState getNextLegalPlayerForLead()
		{
			PlayerState testPlayer = activePlayer;

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

		public boolean isCardLegalToPlay(Card card, PlayerState player)
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
}
