package com.herscher.cribbage.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.herscher.cribbage.R;
import com.herscher.cribbage.comm.HostBluetoothLobby;
import com.herscher.cribbage.model.LocalStuff;

import java.io.IOException;

/**
 * TODO add comments
 */
public class HostLobbyActivity extends Activity
{
	private static final String TAG = "HostLobbyActivity";

	// Look at what needs to go into the fragment. Remember I will lose everything on create,
	// namely connected player

	private BluetoothHostLobbyService bluetoothService;
	private LobbyFragment lobbyFragment;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.host_lobby_activity);

		lobbyFragment = (LobbyFragment) getFragmentManager().findFragmentById(R.id.lobbyFragment);

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
		bindService(new Intent(this, BluetoothHostLobbyService.class),
				bluetoothHostLobbyServiceConnection, Context.BIND_AUTO_CREATE);
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
			// TODO: display error dialog
			e.printStackTrace();
			Toast.makeText(this, "Error while starting listening", Toast.LENGTH_LONG).show();
		}
	}

	private void handlePlayerJoined(HostBluetoothLobby.PlayerConnection playerConnection)
	{
		lobbyFragment.setPlayers(LocalStuff.localPlayer, playerConnection.getPlayer());
	}

	private void handlePlayerQuit()
	{
		lobbyFragment.setPlayers(LocalStuff.localPlayer, null);
	}

	private BluetoothHostLobbyService.Listener bluetoothServiceListener = new
			BluetoothHostLobbyService.Listener()
			{
				@Override
				public void onPlayerJoined(HostBluetoothLobby.PlayerConnection playerConnection)
				{
					handlePlayerJoined(playerConnection);
				}

				@Override
				public void onPlayerQuit(HostBluetoothLobby.PlayerConnection playerConnection)
				{
					handlePlayerQuit();
				}

				@Override
				public void onHostingStopped(IOException cause)
				{
					Toast.makeText(HostLobbyActivity.this, "An exception occurred while hosting",
							Toast.LENGTH_LONG).show();
					// TODO: What to do? Exit the activity? Probably so.
				}

				@Override
				public void onBluetoothDisabled()
				{
					Toast.makeText(HostLobbyActivity.this, "You must enable Bluetooth",
							Toast.LENGTH_LONG).show();
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
}
