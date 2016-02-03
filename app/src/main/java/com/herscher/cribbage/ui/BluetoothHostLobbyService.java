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
import com.herscher.cribbage.comm.HostLobby;
import com.herscher.cribbage.comm.KryoMessageSerializer;
import com.herscher.cribbage.comm.MessageConnection;
import com.herscher.cribbage.comm.RemoteLink;
import com.herscher.cribbage.comm.RemoteMessageConnection;
import com.herscher.cribbage.comm.RemotePlayerBridge;
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
		void onPlayerJoined(Player player);

		void onPlayerQuit(Player player);

		void onErrorListening(IOException error);
	}

	private final static String TAG = "BTHostLobbyService";
	private final static int PLAYER_COUNT = 2;

	private final IBinder binder = new Binder();
	private final List<Listener> listeners = new CopyOnWriteArrayList<>();
	private HostLobby hostLobby;
	private ListenRunnable listenRunnable;

	@Override
	public void onDestroy()
	{
		if (hostLobby != null)
		{
			hostLobby.clearLobby();
			hostLobby.removeListener(hostLobbyListener);
			hostLobby = null;
		}

		if (listenRunnable != null)
		{
			listenRunnable.stop();
			listenRunnable = null;
		}

		listeners.clear();
		super.onDestroy();
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent)
	{
		CribbageGame game = createGame();
		hostLobby = new HostLobby(LocalStuff.localPlayer, game, new Handler());
		hostLobby.addListener(hostLobbyListener);

		try
		{
			restartListenThread();
		}
		catch (IOException e)
		{
			Log.e(TAG, String.format("Error during auto-start of listen thread: %s", e.getMessage()));
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

	public CribbageGame startGame()
	{
		if (getConnectedPlayerBridge() == null)
		{
			throw new IllegalStateException("no player connected");
		}

		hostLobby.startGame();
		return hostLobby.getGame();
	}

	public RemotePlayerBridge getConnectedPlayerBridge()
	{
		return hostLobby == null ? null : hostLobby.getConnectedPlayerBridge();
	}

	public boolean isListening()
	{
		return listenRunnable != null;
	}

	public boolean restartListenThread() throws IOException
	{
		if (listenRunnable != null)
		{
			listenRunnable.stop();
		}

		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (bluetoothAdapter == null)
		{
			return false;
		}

		BluetoothServerSocket serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
					"Cribbage Game 1", BluetoothConstants.RFCOMM_UUID);

		listenRunnable = new ListenRunnable(serverSocket);
		new Thread(listenRunnable, "BTHostLobbyService").start();

		return true;
	}

	private void handleBluetoothConnected(BluetoothSocket bluetoothSocket)
	{
		RemoteLink remoteLink = null;

		try
		{
			remoteLink = new BluetoothRemoteLink(bluetoothSocket);
		}
		catch (IOException e)
		{
			Log.e(TAG, String.format("Error creating BluetoothRemoteLink: %s", e.getMessage()));
		}

		MessageConnection messageConnection = new RemoteMessageConnection(
				new FrameRemoteTransport(remoteLink, new Handler()), new KryoMessageSerializer());
		hostLobby.addConnection(messageConnection);
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

	private HostLobby.Listener hostLobbyListener = new HostLobby.Listener()
	{
		@Override
		public void onPlayerJoined(Player player)
		{
			for (Listener l : listeners)
			{
				l.onPlayerJoined(player);
			}
		}

		@Override
		public void onPlayerQuit(Player player)
		{
			for (Listener l : listeners)
			{
				l.onPlayerQuit(player);
			}
		}
	};

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
				catch (final IOException e)
				{
					if (isOpen)
					{
						Log.e(TAG, String.format("Error accepting Bluetooth socket: %s", e.getMessage()));
						stop();

						new Handler().post(new Runnable()
						{
							@Override
							public void run()
							{
								for (Listener l : listeners)
								{
									l.onErrorListening(e);
								}
							}
						});
					}
				}

				if (socket != null)
				{
					handleBluetoothConnected(socket);
				}
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
}
