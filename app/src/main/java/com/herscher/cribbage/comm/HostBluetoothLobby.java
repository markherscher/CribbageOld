package com.herscher.cribbage.comm;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.comm.message.JoinGameRejectedResponseMessage;
import com.herscher.cribbage.comm.message.Message;
import com.herscher.cribbage.comm.message.PlayerQuitMessage;

import java.io.IOException;

/**
 * TODO add comments
 * <p/>
 */
public class HostBluetoothLobby
{
	public interface Listener
	{
		void onPlayerJoined(PlayerConnection playerConnection);

		void onPlayerQuit(PlayerConnection playerConnection);

		void onHostingStopped(IOException cause);
	}

	private final static String TAG = "HostBluetoothLobby";
	private final Handler handler;
	private final Listener listener;
	private final Player hostPlayer;
	private final Object connectedPlayerLock = new Object();
	private final Object isHostingLock = new Object();
	private ListenRunnable listenRunnable;
	private PlayerConnection connectedPlayer;

	public static class PlayerConnection
	{
		private final Player player;
		private final RemoteMessageConnection messageConnection;

		public PlayerConnection(Player player, RemoteMessageConnection messageConnection)
		{
			this.player = player;
			this.messageConnection = messageConnection;
		}

		public Player getPlayer()
		{
			return player;
		}

		public RemoteMessageConnection getMessageConnection()
		{
			return messageConnection;
		}
	}

	public HostBluetoothLobby(Player hostPlayer, Handler handler, Listener listener)
	{
		if (hostPlayer == null || handler == null || listener == null)
		{
			throw new IllegalArgumentException();
		}

		this.hostPlayer = hostPlayer;
		this.handler = handler;
		this.listener = listener;
	}

	public boolean startHosting(BluetoothServerSocket serverSocket, CribbageGame game)
	{
		synchronized (isHostingLock)
		{
			if (!isHosting())
			{
				listenRunnable = new ListenRunnable(serverSocket, game);
				new Thread(listenRunnable, "HostBluetoothLobby").start();
				return true;
			}
		}

		return false;
	}

	public boolean stopHosting()
	{
		boolean wasStopped = false;

		synchronized (isHostingLock)
		{
			if (isHosting())
			{
				listenRunnable.stop();
				listenRunnable = null;
				wasStopped = true;
			}
		}

		if (wasStopped)
		{
			synchronized (connectedPlayerLock)
			{
				if (connectedPlayer != null)
				{
					connectedPlayer.messageConnection.send(new PlayerQuitMessage(hostPlayer), null);
					connectedPlayer.messageConnection.setCloseWhenEmpty(true);
					connectedPlayer.messageConnection.removeListener(messageConnectionListener);
					connectedPlayer = null;
				}
			}
		}

		return wasStopped;
	}

	public boolean isHosting()
	{
		return listenRunnable != null;
	}

	public PlayerConnection getConnectedPlayer()
	{
		return connectedPlayer;
	}

	private class ListenRunnable implements Runnable
	{
		private final BluetoothServerSocket serverSocket;
		private final LobbyAccepter lobbyAccepter;
		private boolean isOpen;

		public ListenRunnable(BluetoothServerSocket serverSocket, CribbageGame game)
		{
			this.serverSocket = serverSocket;
			lobbyAccepter = new LobbyAccepter(hostPlayer, game);
			isOpen = true;
		}

		@Override
		public void run()
		{
			while (isOpen)
			{
				BluetoothSocket socket = acceptBluetoothSocket();
				final RemoteMessageConnection newConnection = createRemoteMessageConnection(socket);
				boolean shouldAcceptNewConnection = false;

				synchronized (connectedPlayerLock)
				{
					shouldAcceptNewConnection = connectedPlayer == null;
				}

				if (shouldAcceptNewConnection)
				{
					Player newPlayer = null;
					try
					{
						newPlayer = lobbyAccepter.acceptConnection(newConnection);
					}
					catch (IOException e)
					{
						Log.e(TAG, String.format("Error in lobbyAccepter: %s", e.getMessage()));
						handleListeningError(e);
					}

					if (newPlayer != null)
					{
						handlePlayerJoined(newPlayer, newConnection);
					}
				}
				else
				{
					// Lobby is full
					newConnection.send(new JoinGameRejectedResponseMessage("Lobby is full"), null);
					newConnection.setCloseWhenEmpty(true);
				}
			}
		}

		private BluetoothSocket acceptBluetoothSocket()
		{
			BluetoothSocket socket = null;

			try
			{
				socket = serverSocket.accept();
			}
			catch (final IOException e)
			{
				Log.e(TAG, String.format("Error accepting Bluetooth socket: %s", e.getMessage()));
				handleListeningError(e);
			}

			return socket;
		}

		private RemoteMessageConnection createRemoteMessageConnection(BluetoothSocket socket)
		{
			if (!isOpen || socket == null)
			{
				return null;
			}

			RemoteLink remoteLink;
			try
			{
				remoteLink = new BluetoothRemoteLink(socket);
			}
			catch (IOException e)
			{
				Log.e(TAG, String.format("Error creating BluetoothRemoteLink: %s", e.getMessage()));
				handleListeningError(e);
				return null;
			}

			return new RemoteMessageConnection(new FrameRemoteTransport(remoteLink, new Handler()), new KryoMessageSerializer());
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

		private void handlePlayerJoined(final Player newPlayer, RemoteMessageConnection connection)
		{
			if (isOpen)
			{
				synchronized (connectedPlayerLock)
				{
					connectedPlayer = new PlayerConnection(newPlayer, connection);
					connectedPlayer.messageConnection.addListener(messageConnectionListener);
					listener.onPlayerJoined(connectedPlayer);
				}
			}
		}

		private void handleListeningError(final IOException error)
		{
			if (stopHosting())
			{
				listener.onHostingStopped(error);
			}
		}
	}

	private RemoteMessageConnection.Listener messageConnectionListener = new MessageConnection.Listener()
	{
		@Override
		public void onReceived(Message message)
		{
			if (message instanceof PlayerQuitMessage)
			{
				handlePlayerQuit();
			}
		}

		@Override
		public void onReceiveError(final IOException error)
		{
			if (stopHosting())
			{
				Log.e(TAG, String.format("Error recieved: %s", error.getMessage()));
				listener.onHostingStopped(error);
			}
		}

		@Override
		public void onClosed()
		{
			Log.w(TAG, "Remote connection closed unexpectedly");
			handlePlayerQuit();
		}

		private void handlePlayerQuit()
		{
			synchronized (connectedPlayerLock)
			{
				if (connectedPlayer != null)
				{
					Log.i(TAG, String.format("Player %s has quit", connectedPlayer.player.toString()));

					PlayerConnection quitter = connectedPlayer;
					connectedPlayer.messageConnection.removeListener(this);
					connectedPlayer.messageConnection.close();
					connectedPlayer = null;
					HostBluetoothLobby.this.listener.onPlayerQuit(quitter);
				}
			}
		}
	};
}
