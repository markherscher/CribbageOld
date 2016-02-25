package com.herscher.cribbage.ui;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.comm.BluetoothConstants;
import com.herscher.cribbage.comm.HostBluetoothLobby;
import com.herscher.cribbage.model.LocalStuff;
import com.herscher.cribbage.scoring.FifteensPlayScorer;
import com.herscher.cribbage.scoring.PairsPlayScorer;
import com.herscher.cribbage.scoring.PlayScoreProcessor;
import com.herscher.cribbage.scoring.RunsPlayScorer;
import com.herscher.cribbage.scoring.ShowdownScoreProcessor;
import com.herscher.cribbage.scoring.StandardShowdownScoreProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO add comments
 */
public class BluetoothHostLobbyService extends Service
{
	public interface Listener
	{
		void onPlayerJoined(HostBluetoothLobby.PlayerConnection playerConnection);

		void onPlayerQuit(HostBluetoothLobby.PlayerConnection playerConnection);

		void onHostingStopped(IOException cause);

		void onBluetoothDisabled();
	}

	private final static String TAG = "BTHostLobbyService";
	private final static int PLAYER_COUNT = 2;

	private final IBinder binder = new Binder();
	private final List<Listener> listeners = new ArrayList<>();
	private Handler handler;
	private HostBluetoothLobby hostBluetoothLobby;
	private BluetoothServerSocket serverSocket;

	@Override
	public void onCreate()
	{
		handler = new Handler();
		hostBluetoothLobby = new HostBluetoothLobby(LocalStuff.localPlayer, handler, lobbyListener);
	}

	@Override
	public void onDestroy()
	{
		stopListening();

		if (serverSocket != null)
		{
			try
			{
				serverSocket.close();
			}
			catch (IOException e)
			{
				// Oh well
			}
		}

		listeners.clear();
		super.onDestroy();
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent)
	{
		try
		{
			startListening();
		}
		catch (IOException e)
		{
			// Oh well
		}

		return binder;
	}

	public void addListener(Listener l)
	{
		if (l != null && !listeners.contains(l))
		{
			listeners.add(l);
		}
	}

	public void removeListener(Listener l)
	{
		listeners.remove(l);
	}

	public boolean isListening()
	{
		return hostBluetoothLobby.isHosting();
	}

	public boolean startListening() throws IOException
	{
		if (serverSocket == null)
		{
			try
			{
				serverSocket = openServerSocket();
			}
			catch (IOException e)
			{
				Log.e(TAG, String.format("Error creating RFCOMM listening socket: %s", e.getMessage()));
				throw e;
			}

			if (serverSocket == null)
			{
				for (Listener l : listeners)
				{
					l.onBluetoothDisabled();
				}
				return false;
			}
		}

		return hostBluetoothLobby.startHosting(serverSocket, createGame());
	}

	public boolean stopListening()
	{
		return hostBluetoothLobby.stopHosting();
	}

	public HostBluetoothLobby.PlayerConnection getConnectedPlayer()
	{
		return hostBluetoothLobby.getConnectedPlayer();
	}

	private BluetoothServerSocket openServerSocket() throws IOException
	{
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
		{
			return null;
		}

		return bluetoothAdapter.listenUsingRfcommWithServiceRecord("Cribbage Game 1", BluetoothConstants.RFCOMM_UUID);
	}

	private CribbageGame createGame()
	{
		PlayScoreProcessor playScoreProcessor = new PlayScoreProcessor(new FifteensPlayScorer(),
				new PairsPlayScorer(), new RunsPlayScorer());
		ShowdownScoreProcessor showdownScoreProcessor = new StandardShowdownScoreProcessor();

		return new CribbageGame(playScoreProcessor, showdownScoreProcessor, PLAYER_COUNT);
	}

	public class Binder extends android.os.Binder
	{
		public BluetoothHostLobbyService getService()
		{
			return BluetoothHostLobbyService.this;
		}
	}

	private HostBluetoothLobby.Listener lobbyListener = new HostBluetoothLobby.Listener()
	{
		@Override
		public void onPlayerJoined(final HostBluetoothLobby.PlayerConnection playerConnection)
		{
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					for (Listener l : listeners)
					{
						l.onPlayerJoined(playerConnection);
					}
				}
			});
		}

		@Override
		public void onPlayerQuit(final HostBluetoothLobby.PlayerConnection playerConnection)
		{
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					for (Listener l : listeners)
					{
						l.onPlayerQuit(playerConnection);
					}
				}
			});
		}

		@Override
		public void onHostingStopped(final IOException cause)
		{
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					for (Listener l : listeners)
					{
						l.onHostingStopped(cause);
					}
				}
			});
		}
	};
}
