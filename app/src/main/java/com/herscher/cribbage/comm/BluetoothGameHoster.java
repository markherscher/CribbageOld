package com.herscher.cribbage.comm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.herscher.cribbage.Player;
import com.herscher.cribbage.comm.message.Message;
import com.herscher.cribbage.comm.message.PlayerQuitMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO add comments
 */
public class BluetoothGameHoster
{
	private final static String TAG = "BluetoothGameHoster";
	private final static String NAME = "CRIBBAGE GAME";

	private final BluetoothAdapter bluetoothAdapter;
	private final Handler handler;
	private final List<HostHandshake> hostHandshakeList;
	private final List<Listener> listeners;
	private final List<PlayerConnectionInfo> connectedPlayerList;
	private ListenRunnable listenRunnable;

	public BluetoothGameHoster(BluetoothAdapter bluetoothAdapter, Handler handler)
	{
		if (bluetoothAdapter == null || handler == null)
		{
			throw new IllegalArgumentException();
		}

		this.bluetoothAdapter = bluetoothAdapter;
		this.handler = handler;
		hostHandshakeList = new ArrayList<>();
		connectedPlayerList = new ArrayList<>();
		listeners = new CopyOnWriteArrayList<>();
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

	public synchronized void startHosting() throws IOException
	{
		if (listenRunnable == null)
		{
			BluetoothServerSocket serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
					NAME, BluetoothConstants.RFCOMM_UUID);
			listenRunnable = new ListenRunnable(serverSocket);
			new Thread(listenRunnable, "BTGameHoster.Listen").start();
		}
	}

	public synchronized void stopHosting()
	{
		cleanUp();

		synchronized (connectedPlayerList)
		{
			// Close all active connections
			for (PlayerConnectionInfo p : connectedPlayerList)
			{
				// Remove listener first so we don't get the callback about the close
				p.messageConnection.removeListener(p.eventConnectionListener);
				p.messageConnection.close();
			}

			connectedPlayerList.clear();
		}

		// TODO: send player left message to all
	}

	public synchronized void startGame()
	{
		cleanUp();

		// TODO: REMOVE LISTENERS FROM CONNECTED PLAYERS, CLEAR LIST


	}

	public Player[] getConnectedPlayers()
	{
		synchronized (connectedPlayerList)
		{
			Player[] players = new Player[connectedPlayerList.size()];

			for (int i = 0; i < players.length; i++)
			{
				players[i] = connectedPlayerList.get(i).player;
			}

			return players;
		}
	}

	private void cleanUp()
	{
		if (listenRunnable != null)
		{
			listenRunnable.cancel();
			listenRunnable = null;
		}

		synchronized (hostHandshakeList)
		{
			for (HostHandshake hh : hostHandshakeList)
			{
				hh.stop();
				hh.getMessageConnection().close();
			}
		}
	}

	private void handleClientConnected(BluetoothSocket socket)
	{
		RemoteLink remoteLink;

		try
		{
			remoteLink = new BluetoothRemoteLink(socket);
		}
		catch (IOException e)
		{
			Log.e(TAG, String.format("Failed to create RemoteLink for client: %s", e.getMessage()));
			return;
		}

		MessageConnection messageConnection = new RemoteMessageConnection(
				new FrameRemoteTransport(remoteLink, handler), new KryoMessageSerializer());

		HostHandshake hostHandshake = new HostHandshake(messageConnection, hostHandshakeListener, handler);

		synchronized (hostHandshakeList)
		{
			hostHandshakeList.add(hostHandshake);
		}

		hostHandshake.start();
	}

	private void handlePlayerConnectionReady(HostHandshake sender, final Player player)
	{
		Log.i(TAG, String.format("Player %s connection is ready", player.toString()));

		synchronized (hostHandshakeList)
		{
			hostHandshakeList.remove(sender);
		}

		final MessageConnection messageConnection = sender.getMessageConnection();

		synchronized (connectedPlayerList)
		{
			connectedPlayerList.add(new PlayerConnectionInfo(player, messageConnection));
		}

		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				for (Listener l : listeners)
				{
					l.onPlayerJoined(player, messageConnection);
				}
			}
		});
	}

	private class ListenRunnable implements Runnable
	{
		private final BluetoothServerSocket serverSocket;
		private boolean isOpen;

		public ListenRunnable(BluetoothServerSocket serverSocket)
		{
			this.serverSocket = serverSocket;
			isOpen = true;
		}

		@Override
		public void run()
		{
			while (isOpen)
			{
				BluetoothSocket socket = null;

				try
				{
					socket = serverSocket.accept();
				}
				catch (IOException e)
				{
					if (isOpen)
					{
						e.printStackTrace();
					}
				}

				if (socket != null)
				{
					handleClientConnected(socket);
				}
			}
		}

		public void cancel()
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

	private HostHandshake.Listener hostHandshakeListener = new HostHandshake.Listener()
	{
		@Override
		public void onReady(HostHandshake sender, Player player)
		{
			handlePlayerConnectionReady(sender, player);
		}

		@Override
		public void onTimedOut(HostHandshake sender)
		{
			Log.e(TAG, "Handshake with client timed out");

			synchronized (hostHandshakeList)
			{
				hostHandshakeList.remove(sender);
			}
		}

		@Override
		public void onError(HostHandshake sender, IOException error)
		{
			Log.e(TAG, String.format("Error occurred during handshake with client (%s)", error.toString()));

			synchronized (hostHandshakeList)
			{
				hostHandshakeList.remove(sender);
			}
		}
	};

	private class PlayerConnectionInfo
	{
		private final Player player;
		private final MessageConnection messageConnection;

		public PlayerConnectionInfo(Player player, MessageConnection messageConnection)
		{
			if (player == null || messageConnection == null)
			{
				throw new IllegalArgumentException();
			}

			this.player = player;
			this.messageConnection = messageConnection;
			messageConnection.addListener(eventConnectionListener);
		}

		private MessageConnection.Listener eventConnectionListener = new MessageConnection.Listener()
		{
			@Override
			public void onSendComplete(Message message, IOException error)
			{
				// Don't care
			}

			@Override
			public void onReceived(Message message)
			{
				if (message instanceof PlayerQuitMessage)
				{
					PlayerQuitMessage quitMessage = (PlayerQuitMessage) message;
					if (quitMessage.getPlayerId() == player.getId())
					{
						// Received leave game message, so close this connection
						Log.i(TAG, String.format("Player %s has explicitly left", player.toString()));
						messageConnection.close();
					}
				}
			}

			@Override
			public void onReceiveError(IOException error)
			{
				Log.e(TAG, String.format("Receive error for player %s (%s)", player.toString(), error.getMessage()));
				messageConnection.close();
			}

			@Override
			public void onClosed()
			{
				Log.i(TAG, String.format("Connection to player %s closed", player.toString()));

				synchronized (connectedPlayerList)
				{
					connectedPlayerList.remove(PlayerConnectionInfo.this);
				}

				handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						for (Listener l : listeners)
						{
							l.onPlayerLeft(player);
						}
					}
				});
			}
		};
	}

	public interface Listener
	{
		void onPlayerJoined(Player player, MessageConnection connection);

		void onPlayerLeft(Player player);
	}
}
