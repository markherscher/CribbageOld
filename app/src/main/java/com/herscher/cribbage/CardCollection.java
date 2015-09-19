package com.herscher.cribbage;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * TODO COMMENT
 */
public class CardCollection implements Iterable<Card>
{
	private final List<Card> cardList;
	private int index = 0;

	public CardCollection()
	{
		cardList = new ArrayList<>();
	}

	public CardCollection(@NonNull List<Card> listToCopy)
	{
		cardList = new ArrayList<>(listToCopy);
	}

	public CardCollection(@NonNull CardCollection copy)
	{
		cardList = new ArrayList<>(copy.cardList);
	}

	public void shuffle()
	{
		Collections.shuffle(cardList);
	}

	public void removeAll()
	{
		cardList.clear();
		reset();
	}

	public void reset()
	{
		index = 0;
	}

	public void add(@NonNull Card card)
	{
		if (card == null)
		{
			throw new IllegalArgumentException();
		}

		cardList.add(card);
	}

	public Card get(int index)
	{
		return cardList.get(index);
	}

	public Card getNext()
	{
		if (remaining() > 0)
		{
			return get(index++);
		}
		else
		{
			return null;
		}
	}

	public int count()
	{
		return cardList.size();
	}

	public int remaining()
	{
		return count() - index;
	}

	@Override
	public String toString()
	{
		return cardList.size() + "";
	}

	@Override
	public Iterator<Card> iterator()
	{
		return cardList.iterator();
	}
}
