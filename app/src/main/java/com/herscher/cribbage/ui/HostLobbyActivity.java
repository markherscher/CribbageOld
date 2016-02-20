package com.herscher.cribbage.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.herscher.cribbage.Player;
import com.herscher.cribbage.R;
import com.herscher.cribbage.comm.MessageConnection;
import com.herscher.cribbage.comm.RemoteMessageConnection;
import com.herscher.cribbage.comm.message.Message;
import com.herscher.cribbage.comm.message.PlayerQuitMessage;
import com.herscher.cribbage.model.LocalStuff;

import java.io.IOException;

/**
 * TODO add comments
 */
public class HostLobbyActivity extends Activity
{
	private static final String TAG = "HostLobbyActivity";

	private BluetoothHostLobbyService bluetoothService;
	private Handler handler;
	private Player player;
	private RemoteMessageConnection connection;
	private LobbyFragment lobbyFragment;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.host_lobby_activity);

		lobbyFragment = (LobbyFragment) getFragmentManager().findFragmentById(R.id.lobbyFragment);
		handler = new Handler();

		lobbyFragment.setListener(lobbyFragmentListener);
		bindBluetoothService();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		// Going away, so no need for a connection to the service
		unbindBluetoothService();
		lobbyFragment.setListener(null);
	}

	private LobbyFragment.Listener lobbyFragmentListener = new LobbyFragment.Listener()
	{
		@Override
		public void onStartClicked()
		{
			Log.i(TAG, "Start clicked");
		}

		@Override
		public void onCancelClicked()
		{
			Log.i(TAG, "Cancel clicked");

			unbindBluetoothService();
		}
	};

	private void bindBluetoothService()
	{
		bindService(new Intent(this, BluetoothHostLobbyService.class), bluetoothHostLobbyServiceConnection, Context.BIND_AUTO_CREATE);
	}

	private void unbindBluetoothService()
	{
		try
		{
			unbindService(bluetoothHostLobbyServiceConnection);
		}
		catch (IllegalArgumentException e)
		{
			// Oh well
		}
	}

	private void startListeningIfNecessary()
	{
		try
		{
			bluetoothService.startListening();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void handlePlayerJoined(RemoteMessageConnection connection, Player player)
	{
		if (connection != null && player != null)
		{
			this.player = player;
			this.connection = connection;

			this.connection.addListener(connectionListener);
			lobbyFragment.setPlayers(LocalStuff.localPlayer, player);
		}
	}

	private void handlePlayerQuit()
	{
		if (connection != null)
		{
			connection.removeListener(connectionListener);
			connection = null;
			player = null;
		}

		lobbyFragment.setPlayers(LocalStuff.localPlayer, null);
	}

	private BluetoothHostLobbyService.Listener bluetoothServiceListener = new BluetoothHostLobbyService.Listener()
	{
		@Override
		public void onPlayerJoined()
		{

		}

		@Override
		public void onPlayerQuit()
		{

		}

		@Override
		public void onHostingStopped(IOException cause)
		{

		}

		@Override
		public void onBluetoothDisabled()
		{

		}
	};

	private ServiceConnection bluetoothHostLobbyServiceConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			Log.i(TAG, "Connection established to BluetoothHostLobbyService");

			bluetoothService = ((BluetoothHostLobbyService.Binder) service).getService();
			bluetoothService.addListener(bluetoothServiceListener);
			startListeningIfNecessary();
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			Log.i(TAG, "Connection lost to BluetoothHostLobbyService");

			bluetoothService.removeListener(bluetoothServiceListener);
			bluetoothService = null;
		}
	};

	private RemoteMessageConnection.Listener connectionListener = new MessageConnection.Listener()
	{
		@Override
		public void onReceived(Message message)
		{
			if (message instanceof PlayerQuitMessage)
			{
				handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						handlePlayerQuit();
					}
				});
			}
		}

		@Override
		public void onReceiveError(IOException error)
		{
			Log.w(TAG, String.format("Disconnecting due to receive error: %s", error.getMessage()));

			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					handlePlayerQuit();
				}
			});
		}

		@Override
		public void onClosed()
		{
			Log.w(TAG, "MessageConnection unexpectedly closed");

			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					handlePlayerQuit();
				}
			});
		}
	};
}
