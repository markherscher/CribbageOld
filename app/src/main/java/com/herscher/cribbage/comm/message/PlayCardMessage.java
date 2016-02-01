package com.herscher.cribbage.comm.message;

import com.herscher.cribbage.Card;

/**
 * TODO add comments
 */
public class PlayCardMessage extends Message
{
	private final Card card;

	public PlayCardMessage(Card card)
	{
		if (card == null)
		{
			throw new IllegalArgumentException();
		}

		this.card = card;
	}

	public Card getCard()
	{
		return card;
	}

	@Override
	public String toString()
	{
		return String.format("PlayCardMessage: %s", card.toString());
	}
}
