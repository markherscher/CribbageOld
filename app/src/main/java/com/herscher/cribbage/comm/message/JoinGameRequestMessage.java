package com.herscher.cribbage.comm.message;

import com.herscher.cribbage.Player;

/**
 * TODO add comments
 */
public class JoinGameRequestMessage extends Message
{
	private final int protocolVersion;
	private final Player player;

	public JoinGameRequestMessage(int protocolVersion, Player player)
	{
		if (player == null)
		{
			throw new IllegalArgumentException();
		}

		this.protocolVersion = protocolVersion;
		this.player = player;
	}

	public Player getPlayer()
	{
		return player;
	}

	public int getProtocolVersion()
	{
		return protocolVersion;
	}

	@Override
	public String toString()
	{
		return String.format("JoinGameRequestMessage (player %s)", player.toString());
	}
}
