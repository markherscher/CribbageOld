package com.herscher.cribbage;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * TODO add comments
 */
public class PairsPlayScorer implements PlayScorer
{
	@Nullable
	@Override
	public ScoreUnit score(List<Card> cards)
	{
		int index = cards.size() - 1;
		int repeatCount = 1;
		Card.Face compareFace = cards.get(index).getFace();

		index--;
		while (index >= 0)
		{
			if (cards.get(index).getFace() == compareFace)
			{
				repeatCount++;
				index--;
			}
			else
			{
				break;
			}
		}

		if (repeatCount == 1)
		{
			return null;
		}
		else
		{
			return new PairsScoreUnit(compareFace, repeatCount);
		}
	}
}
