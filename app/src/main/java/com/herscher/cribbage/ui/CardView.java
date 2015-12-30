package com.herscher.cribbage.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.herscher.cribbage.Card;

/**
 * TODO add comments
 */
public class CardView extends TextView
{
	private Card card;

	public CardView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setCard(null);
	}

	public void setCard(Card card)
	{
		this.card = card;

		if (card == null)
		{
			setText("");
			setEnabled(false);
		}
		else
		{
			setEnabled(true);
			setText(card.toString());
		}
	}

	public Card getCard()
	{
		return card;
	}
}
