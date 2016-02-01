package com.herscher.cribbage.comm.message;

import com.herscher.cribbage.Card;

/**
 * TODO add comments
 */
public class DiscardCardsMessage extends Message
{
	private final Card[] cards;

	public DiscardCardsMessage(Card[] cards)
	{
		if (cards == null)
		{
			throw new IllegalArgumentException();
		}

		this.cards = cards;
	}

	public Card[] getCards()
	{
		return cards;
	}

	@Override
	public String toString()
	{
		return "DiscardCardsMessage";
	}
}
