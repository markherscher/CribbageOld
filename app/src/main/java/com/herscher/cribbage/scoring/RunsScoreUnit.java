package com.herscher.cribbage.scoring;

import android.support.annotation.NonNull;

import com.herscher.cribbage.Card;

/**
 * TODO add comments
 */
public class RunsScoreUnit implements ScoreUnit
{
	private final Card[] cards;

	public RunsScoreUnit(@NonNull Card[] cards)
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
		return 0;
	}

	@Override
	public String getDescription()
	{
		return null;
	}
}
