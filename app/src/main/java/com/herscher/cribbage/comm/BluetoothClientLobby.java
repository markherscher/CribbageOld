package com.herscher.cribbage.comm;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.model.PlayerBridge;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO add comments
 */
public class BluetoothClientLobby
{
	interface Listener
	{
		void onGameJoined(PlayerBridge hostPlayerBridge, CribbageGame game);

		void onGameJoinRejection();

		void onGameJoinError(IOException error);
	}

	private final static String TAG = "BluetoothClientLobby";
	private final Handler handler;
	private final List<Listener> listeners;
	private final Player player;
	private final ClientHandshake clientHandshake;
	private boolean isRunning;

	public BluetoothClientLobby(MessageConnection messageConnection, Player player, Handler handler)
	{
		if (messageConnection == null || player == null || handler == null)
		{
			throw new IllegalArgumentException();
		}

		this.player = player;
		this.handler = handler;
		listeners = new CopyOnWriteArrayList<>();
		clientHandshake = new ClientHandshake(messageConnection, player, clientHandshakeListener, handler);
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

	public synchronized void join() throws IOException
	{
		if (isRunning)
		{
			throw new IllegalStateException("already joining");
		}

		isRunning = true;
		clientHandshake.start();
	}

	public synchronized void cancelJoin()
	{
		isRunning = false;
		clientHandshake.stop();
	}

	private void handleHostConnected(BluetoothSocket bluetoothSocket)
	{
		/*
		RemoteLink remoteLink;

		try
		{
			remoteLink = new BluetoothRemoteLink(bluetoothSocket);
		}
		catch (IOException e)
		{
			Log.e(TAG, String.format("Failed to create RemoteLink for host: %s", e.getMessage()));
			return;
		}

		MessageConnection messageConnection = new RemoteMessageConnection(
				new FrameRemoteTransport(remoteLink, handler), new KryoMessageSerializer());
		clientHandshake = new ClientHandshake(messageConnection, player, clientHandshakeListener, handler);
		clientHandshake.start();
		*/
	}

	private ClientHandshake.Listener clientHandshakeListener = new ClientHandshake.Listener()
	{
		@Override
		public void onReady(Player[] players, CribbageGame game)
		{
			cancelJoin();

			Player remotePlayer = null;
			for (Player p : players)
			{
				if (!p.equals(player))
				{
					remotePlayer = p;
					break;
				}
			}

			if (remotePlayer == null)
			{
				Log.e(TAG, "Unable to find remote player in response's player lister");

				for (Listener l : listeners)
				{
					l.onGameJoinRejection();
				}
			}
			else
			{
				PlayerBridge playerBridge = new RemotePlayerBridge(remotePlayer,
						clientHandshake.getMessageConnection(), handler);
				for (Listener l : listeners)
				{
					l.onGameJoined(playerBridge, game);
				}
			}
		}

		@Override
		public void onDenied(String reason)
		{
			for (Listener l : listeners)
			{
				l.onGameJoinRejection();
			}

			cancelJoin();
		}

		@Override
		public void onTimedOut()
		{
			IOException error = new IOException("join attempt timed out");
			for (Listener l : listeners)
			{
				l.onGameJoinError(error);
			}

			cancelJoin();
		}

		@Override
		public void onError(IOException error)
		{
			for (Listener l : listeners)
			{
				l.onGameJoinError(error);
			}

			cancelJoin();
		}
	};

	private class ConnectRunnable implements Runnable
	{
		private final BluetoothSocket bluetoothSocket;
		private boolean isRunning;

		public ConnectRunnable(BluetoothSocket bluetoothSocket)
		{
			this.bluetoothSocket = bluetoothSocket;
		}

		@Override
		public void run()
		{
			isRunning = true;

			try
			{
				bluetoothSocket.connect();
			}
			catch (IOException e)
			{
				if (isRunning)
				{
					Log.e(TAG, String.format("Failed to connect to host's bluetooth socket: %s", e.getMessage()));

				}
				cancelJoin();
				return;
			}

			handleHostConnected(bluetoothSocket);
		}

		public void cancel()
		{
			isRunning = false;

			try
			{
				bluetoothSocket.close();
			}
			catch (IOException e)
			{
				// Oh well
			}
		}
	}

}
