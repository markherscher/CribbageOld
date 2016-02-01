package com.herscher.cribbage.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.herscher.cribbage.Card;
import com.herscher.cribbage.CardCollection;
import com.herscher.cribbage.CardDeckFactory;
import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.scoring.FifteensPlayScorer;
import com.herscher.cribbage.scoring.PairsPlayScorer;
import com.herscher.cribbage.scoring.PlayScoreProcessor;
import com.herscher.cribbage.scoring.PlayScorer;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.R;
import com.herscher.cribbage.RulesViolationException;
import com.herscher.cribbage.scoring.RunsPlayScorer;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity
{
	private CribbageGame game;
	private LinearLayout player1Layout;
	private LinearLayout player2Layout;
	private TextView player1Score;
	private TextView player2Score;
	private TextView playCount;
	private TextView state;
	private TextView eventsList;
	private PlayerInfo[] playerInfos;
	private int cardDiscardCount;
	private View.OnClickListener cardClickListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			CardView cardView = (CardView) v;
			Player player = null;

			// Find the matching player
			for (PlayerInfo pi : playerInfos)
			{
				for (int i = 0; i < pi.cardsLayout.getChildCount(); i++)
				{
					if (pi.cardsLayout.getChildAt(i) == v)
					{
						player = pi.player;
						break;
					}
				}
			}

			if (player == null)
			{
				throw new IllegalStateException("player not found for card");
			}

			switch (game.getState())
			{
				case DISCARD:
					discardCard(cardView, player);
					break;
				case PLAY:
					playCard(cardView, player);
					break;
				default:
					break;
			}


		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		playerInfos = new PlayerInfo[2];

		playerInfos[0] = new PlayerInfo();
		playerInfos[0].player = new Player("Player 1", 1);
		playerInfos[0].scoreView = (TextView) findViewById(R.id.player1Score);
		playerInfos[0].cardsLayout = (LinearLayout) findViewById(R.id.player1Cards);

		playerInfos[1] = new PlayerInfo();
		playerInfos[1].player = new Player("Player 2", 2);
		playerInfos[1].scoreView = (TextView) findViewById(R.id.player2Score);
		playerInfos[1].cardsLayout = (LinearLayout) findViewById(R.id.player2Cards);

		state = (TextView) findViewById(R.id.state);
		playCount = (TextView) findViewById(R.id.playCount);
		eventsList = (TextView) findViewById(R.id.eventsList);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.actionRestart)
		{
			restart();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void restart()
	{
		for (PlayerInfo pi : playerInfos)
		{
			pi.player.resetScore();
		}

		PlayScoreProcessor scoringProcessor = new PlayScoreProcessor(new PlayScorer[]
				{new FifteensPlayScorer(), new PairsPlayScorer(), new RunsPlayScorer()});

		if (game != null)
		{
			//game.removeListener(this);
		}

		CardCollection cards = new CardCollection(CardDeckFactory.get52CardDeck());
		cards.shuffle();

		eventsList.setText("");
		showEvent("New game started");

		//game = new CribbageGame(scoringProcessor, new Player[]{playerInfos[0].player, playerInfos[1].player});
		game = null;
		//game.addListener(this);
		//game.startGame(cards);
	}

	private void showEvent(String text)
	{
		eventsList.append(text + "\n");
	}

	private void updateShownState()
	{
		switch (game.getState())
		{
			case DISCARD:
				state.setText("Discarding");
				break;

			case GAME_COMPLETE:
				state.setText("Completed");
				break;

			case PLAY:
				state.setText("Playing");
				break;

			case ROUND_COMPLETE:
				state.setText("Round Complete");
				break;
		}
	}

	private void updateHandViews()
	{
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.weight = 1.0f;
		params.setMargins(10, 10, 10, 10);

		for (PlayerInfo pi : playerInfos)
		{
			pi.cardsLayout.removeAllViews();

			for (Card c : pi.player.getHand())
			{
				CardView cardView = new CardView(this, null);
				cardView.setLayoutParams(params);
				cardView.setCard(c);
				cardView.setOnClickListener(cardClickListener);
				cardView.setBackgroundColor(Color.LTGRAY);

				pi.cardsLayout.addView(cardView);
			}
		}
	}

	/*
	@Override
	public void onRoundStarted()
	{
		updateShownState();
		updateHandViews();
		showEvent("-----------------");
		showEvent("Round started");
	}

	@Override
	public void onPlayStarted(Card cutCard)
	{
		updateShownState();
		showEvent(String.format("Play phase started; cut card is %s", cutCard.toString()));
	}

	@Override
	public void onDiscardRequired(int cardCount)
	{
		cardDiscardCount = cardCount;
		showEvent(String.format("All players must discard %d cards", cardCount));
	}

	@Override
	public void onLeadRequired(Player player)
	{
		updateShownState();
		showEvent(String.format("%s must lead a card", player.getName()));
		playCount.setText(String.format("Count: %d", game.getPlayCount()));
	}

	@Override
	public void onPlayRequired(Player player)
	{
		updateShownState();
		showEvent(String.format("%s must play a card", player.getName()));
		playCount.setText(String.format("Count: %d", game.getPlayCount()));
	}

	@Override
	public void onCardsDiscarded(Player player, Card[] cards)
	{
		showEvent(String.format("%s discarded %d cards", player.getName(), cards.length));
	}

	@Override
	public void onCardPlayed(Player player, Card card)
	{
		showEvent(String.format("%s played %s", player.getName(), card.toString()));

		playCount.setText(String.format("Count: %d", game.getPlayCount()));
	}

	@Override
	public void onScoreChanged(Player player, ScoreUnit reason)
	{
		showEvent(String
				.format("%s gained %d points (%s)", player.getName(), reason.getPoints(), reason
						.getDescription()));

		for (PlayerInfo pi : playerInfos)
		{
			if (pi.player == player)
			{
				pi.scoreView.setText(String
						.format("%s: %d", player.getName(), player.getCurrentScore()));
			}
		}
	}

	@Override
	public void onRoundCompleted()
	{
		updateShownState();
		showEvent("Round is completed");
		showEvent("----");

		CardCollection cards = new CardCollection(CardDeckFactory.get52CardDeck());
		cards.shuffle();
		game.startNewRound(cards);
	}

	@Override
	public void onGameCompleted(Player winningPlayer)
	{
		updateShownState();
		showEvent(String.format("Game is over. %s wins!", winningPlayer.getName()));
	}
	*/

	private void discardCard(CardView cardView, Player player)
	{
		for (PlayerInfo pi : playerInfos)
		{
			if (pi.player == player)
			{
				pi.discardedCards.add(cardView.getCard());
				cardView.setCard(null);

				if (pi.discardedCards.size() >= cardDiscardCount)
				{
					Card[] cards = pi.discardedCards.toArray(new Card[cardDiscardCount]);

					try
					{
						game.discardCards(player, cards);
					}
					catch (RulesViolationException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void playCard(CardView cardView, Player player)
	{
		if (game.isCardLegalToPlay(cardView.getCard(), player))
		{
			try
			{
				game.playCard(player, cardView.getCard());
				cardView.setCard(null);
			}
			catch (RulesViolationException e)
			{
				e.printStackTrace();
			}
		} else
		{
			Toast.makeText(MainActivity.this, "That card is not legal to play", Toast.LENGTH_SHORT);
		}
	}

	private class PlayerInfo
	{
		public Player player;
		public TextView scoreView;
		public LinearLayout cardsLayout;
		public List<Card> discardedCards = new ArrayList<>();
	}
}
