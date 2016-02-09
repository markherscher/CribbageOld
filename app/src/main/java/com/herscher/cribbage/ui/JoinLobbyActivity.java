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


}
