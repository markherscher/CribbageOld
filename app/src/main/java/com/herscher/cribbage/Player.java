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
	private final long id;
	private int slotNumber;

	public Player(String name, long id)
	{
		if (name == null)
		{
			throw new IllegalArgumentException();
		}

		this.name = name;
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public long getId()
	{
		return id;
	}

	public int getSlotNumber()
	{
		return slotNumber;
	}

	public void setSlotNumber(int slotNumber)
	{
		this.slotNumber = slotNumber;
	}

	@Override
	public String toString()
	{
		return String.format("%s [%d]", name, id);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Player)
		{
			return id == ((Player)o).id;
		}
		else
		{
			return false;
		}
	}
}
