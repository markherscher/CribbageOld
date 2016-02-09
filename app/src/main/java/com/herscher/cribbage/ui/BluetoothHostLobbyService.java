package com.herscher.cribbage.ui;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.comm.BluetoothConstants;
import com.herscher.cribbage.comm.BluetoothRemoteLink;
import com.herscher.cribbage.comm.FrameRemoteTransport;
import com.herscher.cribbage.comm.LobbyAccepter;
import com.herscher.cribbage.comm.KryoMessageSerializer;
import com.herscher.cribbage.comm.RemoteLink;
import com.herscher.cribbage.comm.RemoteMessageConnection;
import com.herscher.cribbage.model.LocalStuff;
import com.herscher.cribbage.scoring.FifteensPlayScorer;
import com.herscher.cribbage.scoring.PairsPlayScorer;
import com.herscher.cribbage.scoring.PlayScoreProcessor;
import com.herscher.cribbage.scoring.RunsPlayScorer;
import com.herscher.cribbage.scoring.ShowdownScoreProcessor;
import com.herscher.cribbage.scoring.StandardShowdownScoreProcessor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO add comments
 */
public class BluetoothHostLobbyService extends Service
{
	public interface Listener
	{
		void onListeningCompleted(RemoteMessageConnection connection, Player player, IOException error);
	}

	private final static String TAG = "BTHostLobbyService";
	private final static int PLAYER_COUNT = 2;

	private final IBinder binder = new Binder();
	private final List<Listener> listeners = new CopyOnWriteArrayList<>();
	private Handler handler;
	private ListenRunnable listenRunnable;

	@Override
	public void onCreate()
	{
		handler = new Handler();
	}

	@Override
	public void onDestroy()
	{
		stopListening();

		listeners.clear();
		super.onDestroy();
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent)
	{
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
		return listenRunnable != null;
	}

	public boolean startListening()
	{
		// Only do anything if not already listening
		if (listenRunnable == null)
		{
			BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

			if (bluetoothAdapter == null)
			{
				return false;
			}

			BluetoothServerSocket serverSocket = null;
			try
			{
				serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
						"Cribbage Game 1", BluetoothConstants.RFCOMM_UUID);
			}
			catch (IOException e)
			{
				Log.e(TAG, String.format("Error creating RFCOMM listening socket: %s", e.getMessage()));
				handleListeningComplete(null, null, new IOException("Error creating RFCOMM listening socket", e));
			}

			if (serverSocket != null)
			{
				listenRunnable = new ListenRunnable(serverSocket);
				new Thread(listenRunnable, "BTHostLobbyService").start();
			}
		}

		return true;
	}

	public boolean stopListening()
	{
		if (listenRunnable != null)
		{
			listenRunnable.stop();
			listenRunnable = null;
			return true;
		}

		return false;
	}

	private CribbageGame createGame()
	{
		PlayScoreProcessor playScoreProcessor = new PlayScoreProcessor(new FifteensPlayScorer(),
				new PairsPlayScorer(), new RunsPlayScorer());
		ShowdownScoreProcessor showdownScoreProcessor = new StandardShowdownScoreProcessor();

		return new CribbageGame(playScoreProcessor, showdownScoreProcessor, PLAYER_COUNT);
	}

	private void handleListeningComplete(final RemoteMessageConnection connection, final Player newPlayer, final IOException error)
	{
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (stopListening())
				{
					for (Listener l : listeners)
					{
						l.onListeningCompleted(connection, newPlayer, error);
					}
				}
			}
		});
	}

	public class Binder extends android.os.Binder
	{
		public BluetoothHostLobbyService getService()
		{
			return BluetoothHostLobbyService.this;
		}
	}

	private class ListenRunnable implements Runnable
	{
		private final BluetoothServerSocket serverSocket;
		private final LobbyAccepter lobbyAccepter;
		private boolean isOpen;

		public ListenRunnable(BluetoothServerSocket serverSocket)
		{
			this.serverSocket = serverSocket;
			lobbyAccepter = new LobbyAccepter(LocalStuff.localPlayer, createGame());
			isOpen = true;
		}

		@Override
		public void run()
		{
			BluetoothSocket socket = null;

			try
			{
				socket = serverSocket.accept();
			}
			catch (final IOException e)
			{
				if (isOpen)
				{
					Log.e(TAG, String.format("Error accepting Bluetooth socket: %s", e.getMessage()));
					handleListeningComplete(null, null, e);
				}
			}

			if (!isOpen)
			{
				return;
			}

			RemoteLink remoteLink;
			try
			{
				remoteLink = new BluetoothRemoteLink(socket);
			}
			catch (IOException e)
			{
				Log.e(TAG, String.format("Error creating BluetoothRemoteLink: %s", e.getMessage()));
				handleListeningComplete(null, null, e);
				return;
			}

			if (!isOpen)
			{
				return;
			}

			RemoteMessageConnection messageConnection = new RemoteMessageConnection(
					new FrameRemoteTransport(remoteLink, new Handler()), new KryoMessageSerializer());

			Player newPlayer;
			try
			{
				newPlayer = lobbyAccepter.acceptConnection(messageConnection);
			}
			catch (IOException e)
			{
				Log.e(TAG, String.format("Error in lobbyAccepter: %s", e.getMessage()));
				handleListeningComplete(null, null, e);
				return;
			}

			handleListeningComplete(messageConnection, newPlayer, null);
		}

		public void stop()
		{
			if (isOpen)
			{
				isOpen = false;
				lobbyAccepter.cancelAccept();

				try
				{
					serverSocket.close();
				}
				catch (IOException e)
				{
					// Oh well
				}
			}
		}
	}
}
