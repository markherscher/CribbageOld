package com.herscher.cribbage.model;

import android.os.Handler;

import com.herscher.cribbage.Card;
import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.RulesViolationException;
import com.herscher.cribbage.scoring.ScoreUnit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO add comments
 * <p/>
 * TODO add close
 */
public class CribbageGameController
{
	private final CribbageGame game;
	private final Player localPlayer;
	private final PlayerBridge[] playerBridges;
	private final List<Listener> listeners;
	private final Handler handler;

	public CribbageGameController(CribbageGame game, Player localPlayer, PlayerBridge[] playerBridges)
	{
		if (game == null || localPlayer == null || playerBridges == null)
		{
			throw new IllegalArgumentException();
		}

		this.game = game;
		this.localPlayer = localPlayer;
		this.playerBridges = new PlayerBridge[playerBridges.length];
		listeners = new CopyOnWriteArrayList<>();
		handler = new Handler();

		// Make a copy of the bridges array
		for (int i = 0; i < playerBridges.length; i++)
		{
			this.playerBridges[i] = playerBridges[i];
			this.playerBridges[i].addListener(new PlayerBridgeListener(this.playerBridges[i]));
		}
	}

	public void addListener(Listener l)
	{
		if (l != null && !listeners.contains(l))
		{
			listeners.add(l);
		}
	}

	public void removeListener(Listener l)
	{
		listeners.remove(l);
	}

	public void discardCards(Card... cards) throws RulesViolationException
	{
		discardCardsAndFireEvents(cards, localPlayer);
		// TODO SEND TO BRIDGES
	}

	public void playCard(Card card, GameEventCallback callback) throws RulesViolationException
	{
		playCardAndFireEvents(card, localPlayer);

		// Notify all other players of the change
		final BridgeCallbackAggregator aggregator = new BridgeCallbackAggregator(callback);
		for (final PlayerBridge pb : playerBridges)
		{
			pb.notifyCardsPlayed(card, new PlayerBridge.NotifyCompleteCallback()
			{
				@Override
				public void onCompleted(Exception error)
				{
					aggregator.handleResult(pb, error);
				}
			});
		}
	}

	private void discardCardsAndFireEvents(Card[] cards, Player player) throws RulesViolationException
	{
		game.discardCards(player, cards);
		boolean isPlayStarted = game.getState() != CribbageGame.State.DISCARD;

		for (Listener l : listeners)
		{
			l.onCardsDiscarded(player, cards);

			if (isPlayStarted)
			{
				l.onPlayStarted(game.getCutCard());
			}
		}
	}

	private void playCardAndFireEvents(Card card, Player player) throws RulesViolationException
	{
		ScoreUnit[] scoreUnits = game.playCard(player, card);

		for (Listener l : listeners)
		{
			l.onCardPlayed(player, card, scoreUnits);
		}

		// Now see what effect that had
		boolean isRoundComplete = false;
		boolean isGameComplete = false;

		switch (game.getState())
		{
			case GAME_COMPLETE:
				isGameComplete = true;
				// fallthrough
			case ROUND_COMPLETE:
				isRoundComplete = true;
				break;
			default:
				// Change nothing
				break;
		}

		if (isRoundComplete)
		{
			ScoreUnit[] scores = game.getEndOfRoundScoringForPlayer(player);
			for (Listener l : listeners)
			{
				// TODO
				l.onRoundCompleted(null);

				if (isGameComplete)
				{
					l.onGameCompleted(game.getWinner());
				}
			}

			if (!isGameComplete)
			{
				//game.startNewRound(cardsgohere);
			}
		}
		else
		{
			if (game.getPlayCount() == 0)
			{
				// Someone new to lead
				for (Listener l : listeners)
				{
					l.onLeadRequired(game.getActivePlayer());
				}
			}
			else
			{
				for (Listener l : listeners)
				{
					l.onPlayRequired(game.getActivePlayer());
				}
			}
		}
	}

	// tODO: add event for player rejected action
	public interface Listener
	{
		void onRoundStarted();

		void onPlayStarted(Card cutCard);

		void onDiscardRequired(int cardCount);

		void onPlayRequired(Player player);

		void onLeadRequired(Player player);

		void onCardsDiscarded(Player player, Card[] cards);

		void onCardPlayed(Player player, Card card, ScoreUnit[] scores);

		void onRoundCompleted(PlayerScores[] playerScores);

		void onGameCompleted(Player winningPlayer);

		class PlayerScores
		{
			private final Player player;
			private final ScoreUnit[] scores;

			protected PlayerScores(Player player, ScoreUnit[] scores)
			{
				this.player = player;
				this.scores = scores;
			}

			public Player getPlayer()
			{
				return player;
			}

			public ScoreUnit[] getScores()
			{
				return scores;
			}
		}
	}

	public interface GameEventCallback
	{
		void onSuccessful();

		void onFailedForPlayer(Player player, Exception error);
	}

	private class BridgeCallbackAggregator
	{
		private final GameEventCallback callback;
		private int callbackCount;
		private boolean hasFailed;

		public BridgeCallbackAggregator(GameEventCallback callback)
		{
			this.callback = callback;
		}

		public void handleResult(final PlayerBridge bridge, final Exception result)
		{
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					callbackCount++;

					if (result == null)
					{
						if (!hasFailed && callbackCount >= playerBridges.length)
						{
							callback.onSuccessful();
						}
					}
					else
					{
						// Report the failure
						hasFailed = true;
						callback.onFailedForPlayer(bridge.getPlayer(), result);
					}
				}
			});
		}
	}

	private class PlayerBridgeListener implements PlayerBridge.Listener
	{
		private final PlayerBridge bridge;

		public PlayerBridgeListener(PlayerBridge bridge)
		{
			this.bridge = bridge;
		}

		@Override
		public void onCardsDiscarded(Card[] cards)
		{

		}

		@Override
		public void onCardPlayed(Card card)
		{
			try
			{
				playCardAndFireEvents(card, bridge.getPlayer());
			}
			catch (RulesViolationException e)
			{
				// TODO what to do here
				e.printStackTrace();
			}
		}

		@Override
		public void onRulesViolation(RulesViolationException error)
		{

		}

		@Override
		public void onClosed()
		{
			// TODO
		}
	}
}
