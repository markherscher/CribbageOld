package com.herscher.cribbage;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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
		index = copy.index;
	}

	public void shuffle(long seed)
	{
		Collections.shuffle(cardList, new Random(seed));
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

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof CardCollection)
		{
			CardCollection other = (CardCollection) o;
			if (index != other.index || cardList.size() != cardList.size())
			{
				return false;
			}

			for (int i = 0; i < cardList.size(); i++)
			{
				if (!cardList.get(i).equals(other.cardList.get(i)))
				{
					return false;
				}
			}

			return true;
		}
		else
		{
			return false;
		}
	}
}
