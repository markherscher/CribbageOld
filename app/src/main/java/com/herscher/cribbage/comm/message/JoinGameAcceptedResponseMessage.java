package com.herscher.cribbage.comm.message;

import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.Player;

/**
 * TODO add comments
 */
public class JoinGameAcceptedResponseMessage extends Message
{
	private final Player[] players;
	private final CribbageGame game;

	public JoinGameAcceptedResponseMessage(Player[] players, CribbageGame game)
	{
		if (players == null || game == null)
		{
			throw new IllegalArgumentException();
		}

		this.players = players;
		this.game = game;
	}

	public Player[] getPlayers()
	{
		return players;
	}

	public CribbageGame getGame()
	{
		return game;
	}

	@Override
	public String toString()
	{
		return "JoinGameAcceptedResponseMessage";
	}
}
