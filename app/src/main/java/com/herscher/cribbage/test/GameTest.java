package com.herscher.cribbage.test;

import android.os.Handler;

import com.herscher.cribbage.Card;
import com.herscher.cribbage.CardCollection;
import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.scoring.FifteensPlayScorer;
import com.herscher.cribbage.scoring.PairsPlayScorer;
import com.herscher.cribbage.scoring.PlayScoreProcessor;
import com.herscher.cribbage.scoring.PlayScorer;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.RulesViolationException;
import com.herscher.cribbage.scoring.RunsPlayScorer;
import com.herscher.cribbage.ScoreUnit;

public class GameTest
{
	private final Handler handler;

	public GameTest(Handler handler)
	{
		this.handler = handler;
	}

	public void runTests()
	{
		testListenerCallbacks();
		testLegalDiscard();
		testIllegalDiscard();
		testPlayIsPassedDueToCount();
		testCountIsHitExactly();
		testPlayRemainsOnPlayerDueToCount();
	}

	public void testListenerCallbacks()
	{

	}

	public void testLegalDiscard()
	{

	}

	public void testIllegalDiscard()
	{

	}

	public void testPlayIsPassedDueToCount()
	{
		final CribbageGame game = getDefaultGame();
		CardCollection cards = new CardCollection();
		cards.add(new Card(Card.Suit.CLUBS, Card.Face.KING)); // cut card

		// Player 1
		cards.add(new Card(Card.Suit.CLUBS, Card.Face.TWO)); // discard
		cards.add(new Card(Card.Suit.CLUBS, Card.Face.THREE)); // discard
		cards.add(new Card(Card.Suit.DIAMONDS, Card.Face.NINE)); // #2, 19 pts
		cards.add(new Card(Card.Suit.HEARTS, Card.Face.TEN)); // #4, pass
		cards.add(new Card(Card.Suit.HEARTS, Card.Face.JACK)); // #6, pass
		cards.add(new Card(Card.Suit.DIAMONDS, Card.Face.JACK)); // #8, pass

		// Player 2
		cards.add(new Card(Card.Suit.HEARTS, Card.Face.TWO)); // discard
		cards.add(new Card(Card.Suit.HEARTS, Card.Face.THREE)); // discard
		cards.add(new Card(Card.Suit.SPADES, Card.Face.TEN)); // #1, 10 pts
		cards.add(new Card(Card.Suit.CLUBS, Card.Face.QUEEN)); // #3, 29 pts
		cards.add(new Card(Card.Suit.CLUBS, Card.Face.ACE)); // #5, 30 pts
		cards.add(new Card(Card.Suit.SPADES, Card.Face.JACK)); // #7, pass

		final Player player1 = game.getAllPlayers()[0];
		final Player player2 = game.getAllPlayers()[1];

		CribbageGame.Listener listener = new CribbageGame.Listener()
		{
			boolean expectPlayer2 = true;
			int playCount = 0;
			int leadCount = 0;
			Card cardPlayed = null;

			@Override
			public void onRoundStarted()
			{
			}

			@Override
			public void onPlayStarted(Card cutCard)
			{
			}

			@Override
			public void onDiscardRequired(int cardCount)
			{
			}

			@Override
			public void onLeadRequired(final Player player)
			{
				handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						cardPlayed = player.getHand().get(0);

						try
						{
							switch (leadCount)
							{
								case 0:
									assertTrue(player == player2, "Expected player 2");
									assertTrue(game.getPlayCount() == 0);
									game.playCard(player, cardPlayed); // 10 spades
									assertTrue(game.getPlayCount() == 10);
									break;

								case 1:
									assertTrue(player == player1, "Expected player 1");
									assertTrue(game.getPlayCount() == 0);
									// Don't lead anything; test is over
									break;
							}
						}
						catch (RulesViolationException e)
						{
							assertTrue(false, e.getMessage());
						}

						leadCount++;
					}
				});
			}

			@Override
			public void onPlayRequired(final Player player)
			{
				handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						cardPlayed = player.getHand().get(0);

						try
						{
							switch (playCount)
							{
								case 0:
									assertTrue(player == player1, "Expected player 1");
									assertTrue(game.getPlayCount() == 10);
									game.playCard(player, cardPlayed); // 9 diamonds
									assertTrue(game.getPlayCount() == 19);
									break;
								case 1:
									assertTrue(player == player2, "Expected player 2");
									assertTrue(game.getPlayCount() == 19);
									game.playCard(player, cardPlayed); // Q clubs
									assertTrue(game.getPlayCount() == 29);
									break;
								case 2:
									// player 1 passed
									assertTrue(player == player2, "Expected player 2");
									assertTrue(game.getPlayCount() == 29);
									game.playCard(player, cardPlayed); // A clubs
									assertTrue(game.getPlayCount() == 0); // must lead again
									break;
								default:
									assertTrue(false, "Unexpected play");
									break;
							}
						}
						catch (RulesViolationException e)
						{
							assertTrue(false, e.getMessage());
						}

						playCount++;
					}
				});
			}

			@Override
			public void onCardsDiscarded(Player player, Card[] cards)
			{
			}

			@Override
			public void onCardPlayed(Player player, Card card)
			{
				assertTrue(card == cardPlayed, "Wrong played card for callback");
			}

			@Override
			public void onScoreChanged(Player player, ScoreUnit reason)
			{
			}

			@Override
			public void onRoundCompleted()
			{
			}

			@Override
			public void onGameCompleted(Player winningPlayer)
			{
			}
		};

		game.addListener(listener);
		game.startGame(cards);

		try
		{
			game.discardCards(player1, new Card[]{player1.getHand().get(0), player1.getHand()
					.get(1)});
			game.discardCards(player2, new Card[]{player2.getHand().get(0), player2.getHand()
					.get(1)});
		}
		catch (RulesViolationException e)
		{
			assertTrue(false, e.getMessage());
		}
	}

	private void assertTrue(boolean condition)
	{
		assertTrue(condition, "");
	}

	private void assertTrue(boolean condition, String msg)
	{
		if (!condition)
		{
			throw new IllegalStateException(String.format("Test failed %s", msg));
		}
	}

	public void testCountIsHitExactly()
	{

	}

	public void testPlayRemainsOnPlayerDueToCount()
	{

	}

	private CribbageGame getDefaultGame()
	{
		PlayScoreProcessor scoringProcessor = new PlayScoreProcessor(new PlayScorer[]
				{new FifteensPlayScorer(), new PairsPlayScorer(), new RunsPlayScorer()});
		//return new CribbageGame(scoringProcessor, new Player[]{new Player("Player 1", 1), new Player("Player 2", 2)});
		return null;
	}
}
