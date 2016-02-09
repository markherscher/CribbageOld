package com.herscher.cribbage.comm;

import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.Player;

/**
 * TODO add comments
 */
public class Lobby
{
	private final Player hostPlayer;
	private final Player clientPlayer;
	private final CribbageGame game;

	public Lobby(Player hostPlayer, Player clientPlayer, CribbageGame game)
	{
		if (hostPlayer == null || clientPlayer == null || game == null)
		{
			throw new IllegalArgumentException();
		}

		this.hostPlayer = hostPlayer;
		this.clientPlayer = clientPlayer;
		this.game = game;
	}

	public Player getHostPlayer()
	{
		return hostPlayer;
	}

	public Player getClientPlayer()
	{
		return clientPlayer;
	}

	public CribbageGame getGame()
	{
		return game;
	}
}
