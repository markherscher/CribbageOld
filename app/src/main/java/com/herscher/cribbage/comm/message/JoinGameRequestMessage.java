package com.herscher.cribbage.comm.message;

/**
 * TODO add comments
 */
public class JoinGameRequestMessage extends Message
{
	private final int protocolVersion;
	private final String playerName;
	private final long playerId;

	public JoinGameRequestMessage(int protocolVersion, String playerName, long playerId)
	{
		if (playerName == null)
		{
			playerName = "";
		}

		this.protocolVersion = protocolVersion;
		this.playerName = playerName;
		this.playerId = playerId;
	}

	public int getProtocolVersion()
	{
		return protocolVersion;
	}

	public String getPlayerName()
	{
		return playerName;
	}

	public long getPlayerId()
	{
		return playerId;
	}

	@Override
	public String toString()
	{
		return String.format("JoinGameRequestMessage (player %s", playerName);
	}
}
