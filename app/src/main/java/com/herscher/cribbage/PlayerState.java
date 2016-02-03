package com.herscher.cribbage;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO add comments
 */
public class PlayerState
{
	private final int slot;
	private final List<Card> hand;
	private final List<Card> discardedCards;
	private final List<Card> playedCards;
	private int currentScore;
	private int lastScore;

	public PlayerState(int slot)
	{
		this.slot = slot;
		hand = new ArrayList<>();
		discardedCards = new ArrayList<>();
		playedCards = new ArrayList<>();
	}

	public void resetScore()
	{
		currentScore = 0;
		lastScore = 0;
	}

	public void addScore(int points)
	{
		if (points < 0)
		{
			throw new IllegalArgumentException();
		}

		lastScore = currentScore;
		currentScore += points;
	}

	public int getSlot()
	{
		return slot;
	}

	public int getLastScore()
	{
		return lastScore;
	}

	public int getCurrentScore()
	{
		return currentScore;
	}

	public List<Card> getHand()
	{
		return hand;
	}

	public List<Card> getDiscardedCards() { return discardedCards; }

	public List<Card> getPlayedCards() { return playedCards; }

	@Override
	public String toString()
	{
		return String.format("PlayerState slot %d", slot);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof PlayerState)
		{
			return slot == ((PlayerState) o).slot;
		}
		else
		{
			return false;
		}
	}
}
