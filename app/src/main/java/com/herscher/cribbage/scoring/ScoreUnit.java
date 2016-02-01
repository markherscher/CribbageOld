package com.herscher.cribbage.scoring;

import com.herscher.cribbage.Card;

/**
 * TODO add comments
 */
public interface ScoreUnit
{
	public Card[] getCards();

	int getPoints();

	String getDescription();
}
