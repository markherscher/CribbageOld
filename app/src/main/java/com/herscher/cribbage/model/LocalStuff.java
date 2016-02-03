package com.herscher.cribbage.model;

import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.Player;

import java.util.Random;

/**
 * TODO add comments
 */
public class LocalStuff
{
	// TODO fix this later; this is ugly
	public static Player localPlayer = new Player("Mark Herscher", new Random().nextLong());

	private LocalStuff()
	{

	}
}
