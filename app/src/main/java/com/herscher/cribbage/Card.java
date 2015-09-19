package com.herscher.cribbage;

import android.support.annotation.NonNull;

/**
 * TODO COMMENT
 */
public class Card implements Comparable<Card>
{
	public enum Suit
	{
		CLUBS,
		DIAMONDS,
		HEARTS,
		SPADES
	}

	public enum Face
	{
		TWO(0),
		THREE(1),
		FOUR(2),
		FIVE(3),
		SIX(4),
		SEVEN(5),
		EIGHT(6),
		NINE(7),
		TEN(8),
		JACK(9),
		QUEEN(10),
		KING(11),
		ACE(12);

		private int sortValue;

		private Face(int sortValue)
		{
			this.sortValue = sortValue;
		}
	}

	private final Suit suit;
	private final Face face;

	public Card(@NonNull Suit suit, @NonNull Face face)
	{
		if (suit == null || face == null)
		{
			throw new IllegalArgumentException();
		}

		this.suit = suit;
		this.face = face;
	}

	public Suit getSuit()
	{
		return suit;
	}

	public Face getFace()
	{
		return face;
	}

	@Override
	public String toString()
	{
		return String.format("%s %s", face.toString(), suit.toString());
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Card)
		{
			Card other = (Card) o;
			return suit == other.suit && face == other.face;
		}
		else
		{
			return false;
		}
	}

	@Override
	public int compareTo(Card another)
	{
		if (face.sortValue < another.face.sortValue)
		{
			return -1;
		}
		else if (face.sortValue > another.face.sortValue)
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}
}
