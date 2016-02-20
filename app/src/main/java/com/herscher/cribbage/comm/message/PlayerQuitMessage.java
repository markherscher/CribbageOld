package com.herscher.cribbage.comm.message;

import com.herscher.cribbage.Player;

/**
 * TODO add comments
 */
public class PlayerQuitMessage extends Message
{
	private final Player player;

	public PlayerQuitMessage(Player player)
	{
		if (player == null)
		{
			throw new IllegalArgumentException();
		}

		this.player = player;
	}

	public Player getPlayer()
	{
		return player;
	}
}
