package com.herscher.cribbage;

import android.support.annotation.NonNull;

import java.util.List;

/**
 * TODO add comments
 */
public class DiscardActionHandler implements CribbageGame.ActionHandler
{
	private final static int DISCARD_COUNT = 2;

	private final GameState gameState;

	public DiscardActionHandler(@NonNull GameState gameState)
	{
		this.gameState = gameState;
	}

	@Override
	public void discardCards(@NonNull Player player, @NonNull Card[] cards) throws RulesViolationException
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

		if (gameState.getPlayers().indexOf(player) < 0)
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
			playerHand.remove(c);
			playerDiscard.add(c);
			gameState.getCrib().add(c);
		}
	}

	@Override
	public ScoreUnit[] playCard(Player player, Card card) throws RulesViolationException
	{
		throw new RulesViolationException("state is Discard, not Play");
	}
}
