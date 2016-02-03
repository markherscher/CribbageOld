package com.herscher.cribbage.model;

import android.os.Handler;

import com.herscher.cribbage.Card;
import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.PlayerState;
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
	private final List<Listener> listeners;
	private final Handler handler;

	public CribbageGameController(CribbageGame game)
	{
		if (game == null)
		{
			throw new IllegalArgumentException();
		}

		this.game = game;
		listeners = new CopyOnWriteArrayList<>();
		handler = new Handler();
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

	public void discardCards(PlayerState player, Card... cards) throws RulesViolationException
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

	public void playCard(PlayerState player, Card card) throws RulesViolationException
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

		void onPlayRequired(PlayerState player);

		void onLeadRequired(PlayerState player);

		void onCardsDiscarded(PlayerState player, Card[] cards);

		void onCardPlayed(PlayerState player, Card card, ScoreUnit[] scores);

		void onRoundCompleted(PlayerScores[] playerScores);

		void onGameCompleted(PlayerState winningPlayer);

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
}
