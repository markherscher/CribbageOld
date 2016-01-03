package com.herscher.cribbage;

import android.test.AndroidTestCase;

import com.herscher.cribbage.scoring.FifteensPlayScorer;
import com.herscher.cribbage.scoring.PairsPlayScorer;
import com.herscher.cribbage.scoring.PlayScoreProcessor;
import com.herscher.cribbage.scoring.RunsPlayScorer;
import com.herscher.cribbage.scoring.StandardShowdownScoreProcessor;

import junit.framework.Assert;

public class CribbageGameTest extends AndroidTestCase
{
	public void testNewGame() throws RulesViolationException
	{
		CribbageGame game = createDefaultGame();
		Player[] players = game.getAllPlayers();

		// Initial state is good
		Assert.assertEquals(CribbageGame.State.NEW, game.getState());

		// Can't start without enough cards
		CardCollection cards = new CardCollection(CardDeckFactory.get52CardDeck());
		while (cards.remaining() >= game.getMinimumDealCount())
		{
			cards.getNext();
		}

		try
		{
			game.startGame(cards);
			Assert.assertTrue(false);
		}
		catch (RulesViolationException e)
		{
			// Passed
		}

		// State did not change after failed start
		Assert.assertEquals(CribbageGame.State.NEW, game.getState());

		// Can't call any invalid public functions
		try
		{
			game.startNewRound(cards);
			Assert.assertTrue(false);
		}
		catch (RulesViolationException e)
		{
			// Passed
		}
		try
		{
			game.discardCards(players[0], new Card(), new Card());
			Assert.assertTrue(false);
		}
		catch (RulesViolationException e)
		{
			// Passed
		}
		try
		{
			game.playCard(game.getAllPlayers()[0], new Card());
			Assert.assertTrue(false);
		}
		catch (RulesViolationException e)
		{
			// Passed
		}

		// Test valid start
		cards.reset();
		game.startGame(cards);
		Assert.assertEquals(CribbageGame.State.DISCARD, game.getState());

		Assert.assertEquals(null, game.getActivePlayer());
		Assert.assertEquals(players[0], game.getDealingPlayer());
		Assert.assertEquals(0, game.getCrib().length);
		Assert.assertEquals(cards.get(0), game.getCutCard());
		Assert.assertEquals(null, game.getWinner());
		Assert.assertEquals(0, game.getPlayCount());

		for (Player p : players)
		{
			Assert.assertEquals(0, p.getCurrentScore());
			Assert.assertEquals(0, p.getLastScore());
			Assert.assertEquals(0, p.getDiscardedCards().size());
			Assert.assertEquals(0, p.getPlayedCards().size());
			Assert.assertEquals(6, p.getHand().size());
		}
	}

	public void testLegalToPlay()
	{

	}

	private static Player[] getTwoPlayers()
	{
		Player player1 = new Player("player 1", 55);
		Player player2 = new Player("player 2", 23);

		return new Player[] { player1, player2} ;
	}

	private static CribbageGame createDefaultGame()
	{
		PlayScoreProcessor psp = new PlayScoreProcessor(new FifteensPlayScorer(), new PairsPlayScorer(), new RunsPlayScorer());
		return new CribbageGame(psp, new StandardShowdownScoreProcessor(), getTwoPlayers());
	}
}
