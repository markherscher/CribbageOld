package com.herscher.cribbage;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO COMMENT
 */
public class Player
{
	private final String name;
	private final List<Card> hand;
	private final List<Card> discardedCards;
	private final List<Card> playedCards;
	private final int id;
	private int currentScore;
	private int lastScore;

	public Player(@NonNull String name, int id)
	{
		if (name == null)
		{
			throw new IllegalArgumentException();
		}

		this.name = name;
		this.id = id;
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

	public int getLastScore()
	{
		return lastScore;
	}

	public int getCurrentScore()
	{
		return currentScore;
	}

	public String getName()
	{
		return name;
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
		return name;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Player)
		{
			Player other = (Player) o;
			return id == other.id;
		}
		else
		{
			return false;
		}
	}
}
