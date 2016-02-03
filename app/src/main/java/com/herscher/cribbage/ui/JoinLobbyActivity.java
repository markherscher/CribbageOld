package com.herscher.cribbage.ui;

import android.app.Activity;
import android.os.Bundle;

import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.model.PlayerBridge;

import java.io.IOException;

/**
 * TODO add comments
 */
public class JoinLobbyActivity extends Activity
{
	private static ClientLobby clientLobby;

	public static void setClientLobby(ClientLobby clientLobby)
	{
		JoinLobbyActivity.clientLobby = clientLobby;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (clientLobby != null)
		{
			clientLobby.addListener(clientLobbyListener);
		}
	}

	private ClientLobby.Listener clientLobbyListener = new ClientLobby.Listener()
	{
		@Override
		public void onGameJoined(PlayerBridge hostPlayerBridge, CribbageGame game)
		{

		}

		@Override
		public void onGameJoinRejection()
		{

		}

		@Override
		public void onGameJoinError(IOException error)
		{

		}
	};
}
