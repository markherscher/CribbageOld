package com.herscher.cribbage.comm.message;

/**
 * TODO add comments
 */
public class PlayerQuitMessage extends Message
{
	private final long playerId;

	public PlayerQuitMessage(long playerId)
	{
		this.playerId = playerId;
	}

	public long getPlayerId()
	{
		return playerId;
	}
}
