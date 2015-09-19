package com.herscher.cribbage;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO add comments
 * PlayScoringProcessor
 * PlayScorer
 *
 */
public class StandardPlayScoringProcessor
{
	private final static int MAX_CARD_TOTAL = 31;

	private final List<Card> playedCards;
	private final PlayScorer[] scorers;
	private Player player;
	private int total;

	public StandardPlayScoringProcessor(PlayScorer[] scorers)
	{
		this.scorers = scorers;
		playedCards = new ArrayList<>();
	}

	public ScoreUnit[] playCard(@NonNull Card card) throws RulesViolationException
	{
		if (!isCardLegalToPlay(card))
		{
			throw new RulesViolationException("card is not legal to play");
		}

		playedCards.add(card);
		total += getValue(card);

		List<ScoreUnit> scores = new ArrayList<>();

		for (PlayScorer ps : scorers)
		{
			ScoreUnit scoreUnit = ps.score(playedCards);

			if (scoreUnit != null)
			{
				scores.add(scoreUnit);
			}
		}

		return scores.toArray(new ScoreUnit[scores.size()]);
	}

	public void setPlayer(@NonNull Player player)
	{
		this.player = player;
	}

	public boolean isCardLegalToPlay(@NonNull Card card)
	{
		return getValue(card) + total <= MAX_CARD_TOTAL;
	}

	public int getTotal()
	{
		return total;
	}

	public void reset()
	{
		total = 0;
		playedCards.clear();
	}


	private int getValue(Card card)
	{
		switch (card.getFace())
		{
			case ACE:
				return 1;
			case TWO:
				return 2;
			case THREE:
				return 3;
			case FOUR:
				return 4;
			case FIVE:
				return 5;
			case SIX:
				return 6;
			case SEVEN:
				return 7;
			case EIGHT:
				return 8;
			case NINE:
				return 9;
			case TEN:
			case JACK:
			case QUEEN:
			case KING:
				return 10;
			default:
				return 0;
		}
	}
}
