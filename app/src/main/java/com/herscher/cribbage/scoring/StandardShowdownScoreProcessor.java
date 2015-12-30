package com.herscher.cribbage.scoring;

import com.herscher.cribbage.Card;
import com.herscher.cribbage.ScoreUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO add comments
 * TODO: score knobs
 * TODO: score flipping over a jack
 */
public class StandardShowdownScoreProcessor implements ShowdownScoreProcessor
{
	@Override
	public ScoreUnit[] calculateScore(List<Card> cards, Card cutCard)
	{
		// Sorting this makes much of the scoring simpler
		List<Card> sortedList = new ArrayList<>(cards);
		sortedList.add(cutCard);
		Collections.sort(sortedList);

		return new ScoreUnit[0];
	}

	public List<ScoreUnit> scorePairs(List<Card> sortedList)
	{
		List<ScoreUnit> scores = new ArrayList<>();
		Card.Face currentFace = null;
		int repeatCount = 1;

		// Since the list is sorted only one simple pass is required
		for (Card c : sortedList)
		{
			if (currentFace != c.getFace())
			{
				if (repeatCount > 1)
				{
					// TODO
					//scores.add(new PairsScoreUnit(currentFace, repeatCount));
				}


				currentFace = c.getFace();
				repeatCount = 1;
			}
			else
			{
				repeatCount++;
			}
		}

		return scores;
	}

	public List<ScoreUnit> scoreFifteens(List<Card> sortedList)
	{
		List<ScoreUnit> scores = new ArrayList<>();

		// Compare current card to next card

		return null;
	}

	public List<ScoreUnit> scoreRuns(List<Card> sortedList)
	{
		List<ScoreUnit> scores = new ArrayList<>();
		int runLength = 0;
		int lastOrder = 0;

		// Remember: list is sorted
		for (Card c : sortedList)
		{
			int currentCardOrder = getOrder(c);

			if (runLength == 0)
			{
				lastOrder = currentCardOrder;
			}
			else if (currentCardOrder == lastOrder)
			{
				// Same card face; doesn't break a run
			}
			else if (currentCardOrder == lastOrder + 1)
			{
				// Next card in run
				runLength++;
				lastOrder = currentCardOrder;
			}
			else
			{
				// Card breaks run
				if (runLength >= 3)
				{
					// TODO add to lsit
				}
			}
		}

		return scores;
	}

	public ScoreUnit scoreFlush(List<Card> sortedList)
	{
		// TODO
		return null;
	}

	private int getOrder(Card card)
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
				return 10;
			case JACK:
				return 11;
			case QUEEN:
				return 12;
			case KING:
				return 13;
			default:
				return 0;
		}
	}
}
