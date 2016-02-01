package com.herscher.cribbage.scoring;

import android.support.annotation.NonNull;

import com.herscher.cribbage.Card;

/**
 * TODO add comments
 */
public class PairsScoreUnit implements ScoreUnit
{
	private final Card[] cards;
	private final int points;

	public PairsScoreUnit(@NonNull Card[] cards)
	{
		this.cards = cards;

		switch (cards.length)
		{
			case 2:
				points = 2;
				break;

			case 3:
				points = 6;
				break;

			case 4:
				points = 12;
				break;

			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public Card[] getCards()
	{
		return cards;
	}

	@Override
	public int getPoints()
	{
		return points;
	}

	@Override
	public String getDescription()
	{
		return String.format("%d x %s", cards.length, cards[0].getFace().toString());
	}
}
