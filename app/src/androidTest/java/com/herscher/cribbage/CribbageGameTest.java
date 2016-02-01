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

	public void testDiscardValiation() throws RulesViolationException
	{
		CribbageGame game = createDefaultGame();
		Player[] players = game.getAllPlayers();
		CardCollection cards = new CardCollection(CardDeckFactory.get52CardDeck());

		game.startGame(cards);

		Card[] fewCards = new Card[game.getCardsPerDiscard() - 1];
		Card[] discardCards = new Card[game.getCardsPerDiscard()];
		for (int i = 0; i < fewCards.length; i++)
		{
			fewCards[i] = new Card();
		}
		for (int i = 0; i < discardCards.length; i++)
		{
			discardCards[i] = players[0].getHand().get(i);
		}

		// Initial state is good
		Assert.assertEquals(CribbageGame.State.DISCARD, game.getState());

		// Try discarding invalid number of cards
		try
		{
			game.discardCards(players[0], fewCards);
			Assert.assertTrue(false);
		}
		catch (RulesViolationException e)
		{
			// Good
		}
		Assert.assertEquals(CribbageGame.State.DISCARD, game.getState());

		// Try discarding with unknown player or cards
		try
		{
			game.discardCards(new Player("invalid", 32222), discardCards);
		}
		catch (RulesViolationException e)
		{
			// Good
		}
		Card originalCard = discardCards[0];
		discardCards[0] = new Card();
		try
		{
			game.discardCards(players[0], discardCards);
		}
		catch (RulesViolationException e)
		{
			// Good
			discardCards[0] = originalCard;
		}
		Assert.assertEquals(CribbageGame.State.DISCARD, game.getState());

		// Try discarding twice
		game.discardCards(players[0], discardCards);
		try
		{
			game.discardCards(players[0], discardCards);
		}
		catch (RulesViolationException e)
		{
			// Good
		}

		Assert.assertEquals(CribbageGame.State.DISCARD, game.getState());
	}

	public void testDiscardSuccessful() throws RulesViolationException
	{
		CribbageGame game = createDefaultGame();
		Player[] players = game.getAllPlayers();
		CardCollection cards = new CardCollection(CardDeckFactory.get52CardDeck());

		game.startGame(cards);

		Card[] player1Cards = new Card[game.getCardsPerDiscard()];
		Card[] player2Cards = new Card[game.getCardsPerDiscard()];
		for (int i = 0; i < player2Cards.length; i++)
		{
			player1Cards[i] = players[0].getHand().get(i);
			player2Cards[i] = players[1].getHand().get(i);
		}

		game.discardCards(players[0], player1Cards);
		game.discardCards(players[1], player2Cards);

		Assert.assertEquals(CribbageGame.State.PLAY, game.getState());
		Assert.assertEquals(players[1], game.getActivePlayer());
		Assert.assertEquals(players[0], game.getDealingPlayer());
		Assert.assertEquals(4, game.getCrib().length);
		Assert.assertEquals(cards.get(0), game.getCutCard());
		Assert.assertEquals(null, game.getWinner());
		Assert.assertEquals(0, game.getPlayCount());

		for (Player p : players)
		{
			Assert.assertEquals(0, p.getCurrentScore());
			Assert.assertEquals(0, p.getLastScore());
			Assert.assertEquals(2, p.getDiscardedCards().size());
			Assert.assertEquals(0, p.getPlayedCards().size());
			Assert.assertEquals(4, p.getHand().size());
		}
	}

	public void testPlay() throws RulesViolationException
	{
		CribbageGame game = createDefaultGame();
		Player[] players = game.getAllPlayers();
		CardCollection cards = new CardCollection(CardDeckFactory.get52CardDeck());

		game.startGame(cards);

		Card[] player1Cards = new Card[game.getCardsPerDiscard()];
		Card[] player2Cards = new Card[game.getCardsPerDiscard()];
		for (int i = 0; i < player2Cards.length; i++)
		{
			player1Cards[i] = players[0].getHand().get(i);
			player2Cards[i] = players[1].getHand().get(i);
		}

		game.discardCards(players[0], player1Cards);
		game.discardCards(players[1], player2Cards);
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
