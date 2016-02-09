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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.herscher.cribbage.Player;
import com.herscher.cribbage.R;
import com.herscher.cribbage.comm.MessageConnection;
import com.herscher.cribbage.comm.RemoteMessageConnection;
import com.herscher.cribbage.comm.message.Message;
import com.herscher.cribbage.comm.message.PlayerQuitMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO add comments
 */
public class HostLobbyActivity extends Activity implements View.OnClickListener
{
	private static final String TAG = "HostLobbyActivity";

	private final List<String> playerList = new ArrayList<>();
	private ArrayAdapter<String> playerListAdapter;
	private BluetoothHostLobbyService bluetoothService;
	private Handler handler;
	private Player player;
	private RemoteMessageConnection connection;
	private ListView playerListView;
	private Button startButton;
	private Button cancelButton;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.host_lobby_activity);

		playerListView = (ListView) findViewById(R.id.playerList);
		startButton = (Button) findViewById(R.id.startButton);
		cancelButton = (Button) findViewById(R.id.cancelButton);
		handler = new Handler();
		playerListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, playerList);

		startButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);

		bindBluetoothService();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		// Going away, so no need for a connection to the service
		unbindBluetoothService();
	}

	@Override
	public void onClick(View v)
	{
		if (v == startButton)
		{

		}
		else if (v == cancelButton)
		{
			unbindBluetoothService();
		}
	}

	private void bindBluetoothService()
	{
		bindService(new Intent(this, BluetoothHostLobbyService.class), bluetoothHostLobbyServiceConnection, Context.BIND_AUTO_CREATE);
	}

	private void unbindBluetoothService()
	{
		unbindService(bluetoothHostLobbyServiceConnection);
	}

	private void startListeningIfNecessary()
	{
		if (player == null)
		{
			// No connected player, so start listening
			if (!bluetoothService.startListening())
			{
				// TODO: handle bluetooth being disabled
			}
		}
	}

	private void handlePlayerJoined(RemoteMessageConnection connection, Player player)
	{
		if (connection != null && player != null)
		{
			this.player = player;
			this.connection = connection;

			this.connection.addListener(connectionListener);
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

		startListeningIfNecessary();
	}

	private BluetoothHostLobbyService.Listener bluetoothServiceListener = new BluetoothHostLobbyService.Listener()
	{
		@Override
		public void onListeningCompleted(RemoteMessageConnection connection, Player player, IOException error)
		{
			// Don't care if there was an error
			if (error == null)
			{
				handlePlayerJoined(connection, player);
			}
		}
	};

	private ServiceConnection bluetoothHostLobbyServiceConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			bluetoothService = ((BluetoothHostLobbyService.Binder) service).getService();
			bluetoothService.addListener(bluetoothServiceListener);
			startListeningIfNecessary();
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			// Right now it's dead
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
