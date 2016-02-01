package com.herscher.cribbage.scoring;

import com.herscher.cribbage.Card;

/**
 * TODO add comments
 */
public class FifteensScoreUnit implements ScoreUnit
{
	private final Card[] cards;

	public FifteensScoreUnit()
	{
		cards = null;
	}

	public FifteensScoreUnit(Card[] cards)
	{
		this.cards = cards;
	}

	@Override
	public Card[] getCards()
	{
		return cards;
	}

	@Override
	public int getPoints()
	{
		return 2;
	}

	@Override
	public String getDescription()
	{
		return "fifteen";
	}
}
