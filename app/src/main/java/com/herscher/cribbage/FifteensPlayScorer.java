package com.herscher.cribbage;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * TODO add comments
 */
public class FifteensPlayScorer implements PlayScorer
{
	private final static int FIFTEENS_TOTAL = 15;

	@Nullable
	@Override
	public ScoreUnit score(List<Card> cards)
	{
		int index = cards.size() - 1;
		int total = 0;

		while (index >= 0)
		{
			total += getValue(cards.get(index));

			if (total == FIFTEENS_TOTAL)
			{
				return new FifteensScoreUnit();
			}
			else if (total > FIFTEENS_TOTAL)
			{
				break;
			}
		}

		return null;
	}

	protected int getValue(Card card)
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
