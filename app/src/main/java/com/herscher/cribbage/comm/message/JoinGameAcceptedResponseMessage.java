package com.herscher.cribbage.comm.message;

import com.herscher.cribbage.comm.Lobby;

/**
 * TODO add comments
 */
public class JoinGameAcceptedResponseMessage extends Message
{
	private final Lobby lobby;

	public JoinGameAcceptedResponseMessage(Lobby lobby)
	{
		if (lobby == null)
		{
			throw new IllegalArgumentException();
		}

		this.lobby = lobby;
	}

	public Lobby getLobby()
	{
		return lobby;
	}

	@Override
	public String toString()
	{
		return "JoinGameAcceptedResponseMessage";
	}
}
