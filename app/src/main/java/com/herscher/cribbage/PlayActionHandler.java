package com.herscher.cribbage;

import android.support.annotation.NonNull;

import com.herscher.cribbage.scoring.PlayScoreProcessor;

import java.util.List;

/**
 * TODO add comments
 */
public class PlayActionHandler implements CribbageGame.ActionHandler
{
	private final GameState gameState;
	private final PlayScoreProcessor playScoreProcessor;

	public PlayActionHandler(@NonNull GameState gameState, @NonNull PlayScoreProcessor playScoreProcessor)
	{
		this.gameState = gameState;
		this.playScoreProcessor = playScoreProcessor;
	}

	@Override
	public void discardCards(Player player, Card[] cards) throws RulesViolationException
	{
		throw new RulesViolationException("state is Play not Discard");
	}

	@Override
	public ScoreUnit[] playCard(@NonNull Player player, @NonNull Card card) throws RulesViolationException
	{
		List<Card> playerHand = player.getHand();
		List<Card> playerPlayedCards = player.getPlayedCards();

		if (!player.equals(gameState.getActivePlayer()))
		{
			throw new RulesViolationException("player is not the active player");
		}

		if (!isCardLegalToPlay(card, gameState.getActivePlayer()))
		{
			throw new RulesViolationException("card is not legal to play");
		}

		playerHand.remove(card);
		playerPlayedCards.add(card);

		// TODO: Score all at end of round
		return playScoreProcessor.playCard(card);
	}

	public boolean isCardLegalToPlay(Card card, Player player)
	{
		return player.getHand().contains(card) && playScoreProcessor.isCardLegalToPlay(card);
	}
}
