package com.herscher.cribbage.comm;

import android.bluetooth.BluetoothServerSocket;
import android.os.Handler;
import android.util.Log;

import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.Player;

import java.io.IOException;

/**
 * TODO add comments
 * <p/>
 */
public class HostBluetoothLobby
{
	NOTHING IS HAPPENING WITH THIS LISTENER YET
	public interface Listener
	{
		void onPlayerJoined(LobbyBridge lobbyBridge);

		void onPlayerQuit(LobbyBridge lobbyBridge);

		void onHostingStopped(IOException cause);
	}

	private final static String TAG = "HostBluetoothLobby";
	private final Handler handler;
	private final Listener listener;
	private final Player hostPlayer;
	private final Object connectedLobbyLock = new Object();
	private final Object isHostingLock = new Object();
	private ListenRunnable listenRunnable;
	private LobbyBridge connectedLobby;

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
			synchronized (connectedLobbyLock)
			{
				quitAndNotifyOthers();
			}
		}

		return wasStopped;
	}

	public boolean isHosting()
	{
		return listenRunnable != null;
	}

	public LobbyBridge getConnectedLobby()
	{
		return connectedLobby;
	}

	private void quitAndNotifyOthers()
	{
		if (connectedLobby != null)
		{
			// TODO: REMOVE ANY LISTENERS
			connectedLobby.sendPlayerQuit(hostPlayer);
			connectedLobby = null;
		}
	}

	private class ListenRunnable implements Runnable
	{
		private final BluetoothServerSocket serverSocket;
		private final BluetoothConnectionAccepter accepter;
		private final CribbageGame game;
		private boolean isOpen;

		public ListenRunnable(BluetoothServerSocket serverSocket, CribbageGame game)
		{
			this.serverSocket = serverSocket;
			this.game = game;
			accepter = new BluetoothConnectionAccepter(handler);
			isOpen = true;
		}

		@Override
		public void run()
		{
			while (isOpen)
			{
				RemoteMessageConnection newConnection = null;

				try
				{
					newConnection = accepter.acceptIncoming(serverSocket);
				}
				catch (IOException e)
				{
					if (isOpen)
					{
						e.printStackTrace();
					}
				}

				HostHandshaker hostHandshaker = new HostHandshaker(newConnection, hostPlayer, game,
						handler);

				hostHandshaker.waitForJoinRequest(new HostHandshakerListener());
			}
		}

		public void stop()
		{
			if (isOpen)
			{
				isOpen = false;

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

	private class HostHandshakerListener implements HostHandshaker.Listener
	{
		@Override
		public void onJoinRequestReceived(HostHandshaker sender)
		{
			synchronized (connectedLobbyLock)
			{
				if (connectedLobby == null)
				{
					sender.acceptJoinRequest();
					Player joiningPlayer = sender.getJoiningPlayer();
					RemoteMessageConnection messageConnection = sender.getMessageConnection();

					if (joiningPlayer != null && messageConnection != null)
					{
						connectedLobby = new LobbyBridge(joiningPlayer, messageConnection);
						// TODO: add listener here
					}
				}
				else
				{
					sender.rejectJoinRequest("lobby is full");
				}
			}
		}

		@Override
		public void onIoException(HostHandshaker sender, IOException error)
		{
			Log.e(TAG, String.format("Handshake error: %s", error.getMessage()));
		}

		@Override
		public void onTimedOut(HostHandshaker sender)
		{
			Log.e(TAG, "Handshake timed out");
		}
	}
}
