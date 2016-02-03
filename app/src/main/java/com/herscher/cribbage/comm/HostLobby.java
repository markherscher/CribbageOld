package com.herscher.cribbage.comm;

import android.os.Handler;
import android.util.Log;

import com.herscher.cribbage.Card;
import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.RulesViolationException;
import com.herscher.cribbage.model.PlayerBridge;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO add comments
 */
public class HostLobby
{
	public interface Listener
	{
		void onPlayerJoined(Player player);

		void onPlayerQuit(Player player);
	}

	private final static String TAG = "HostLobby";

	private final Player hostPlayer;
	private final CribbageGame game;
	private final Handler handler;
	private final List<Listener> listeners;
	private final Object bridgeLock;
	private final Object activeHandshakeLock;
	private HostHandshake activeHandshake;
	private RemotePlayerBridge connectedPlayerBridge;
	private PlayerBridgeListener playerBridgeListener;

	public HostLobby(Player hostPlayer, CribbageGame game, Handler handler)
	{
		if (hostPlayer == null || game == null || handler == null)
		{
			throw new IllegalArgumentException();
		}

		this.hostPlayer = hostPlayer;
		this.game = game;
		this.handler = handler;
		listeners = new CopyOnWriteArrayList<>();
		bridgeLock = new Object();
		activeHandshakeLock = new Object();
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

	public boolean addConnection(MessageConnection messageConnection)
	{
		synchronized (activeHandshakeLock)
		{
			if (activeHandshake == null)
			{
				activeHandshake = new HostHandshake(messageConnection, hostPlayer, game, hostHandshakeListener, handler);
				activeHandshake.start();
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	public synchronized void clearLobby()
	{
		abortHandshake();

		synchronized (bridgeLock)
		{
			if (connectedPlayerBridge != null)
			{
				connectedPlayerBridge.notifyQuit(new QuitCallback(connectedPlayerBridge));
				connectedPlayerBridge = null;
			}
		}
	}

	public synchronized PlayerBridge startGame()
	{
		abortHandshake();

		PlayerBridge playerBridge;

		synchronized (bridgeLock)
		{
			playerBridge = connectedPlayerBridge;
			connectedPlayerBridge = null;
		}

		if (playerBridge != null)
		{
			// Remove listener because we're done with this instance
			playerBridge.removeListener(playerBridgeListener);
		}

		return playerBridge;
	}

	public RemotePlayerBridge getConnectedPlayerBridge()
	{
		synchronized (bridgeLock)
		{
			return connectedPlayerBridge;
		}
	}

	public CribbageGame getGame()
	{
		return game;
	}

	private void abortHandshake()
	{
		synchronized (activeHandshakeLock)
		{
			if (activeHandshake != null)
			{
				activeHandshake.stop();
				activeHandshake.getMessageConnection().close();
			}
		}
	}

	private void handlePlayerConnectionReady(HostHandshake sender, final Player player)
	{
		Log.i(TAG, String.format("Player %s connection is ready", player.toString()));

		synchronized (activeHandshakeLock)
		{
			activeHandshake = null;
		}

		synchronized (bridgeLock)
		{
			connectedPlayerBridge = new RemotePlayerBridge(player, sender.getMessageConnection(), handler);
			playerBridgeListener = new PlayerBridgeListener(connectedPlayerBridge);
			connectedPlayerBridge.addListener(playerBridgeListener);
		}

		for (Listener l : listeners)
		{
			l.onPlayerJoined(player);
		}
	}

	private class PlayerBridgeListener implements PlayerBridge.Listener
	{
		private final PlayerBridge playerBridge;

		public PlayerBridgeListener(PlayerBridge playerBridge)
		{
			this.playerBridge = playerBridge;
		}

		@Override
		public void onCardsDiscarded(Card[] cards)
		{
			// Don't care
		}

		@Override
		public void onCardPlayed(Card card)
		{
			// Don't care
		}

		@Override
		public void onRulesViolation(RulesViolationException error)
		{
			// Don't care
		}

		@Override
		public void onQuit()
		{
			Log.i(TAG, String.format("Player %s explicitly quit", playerBridge.getPlayer().toString()));
			cleanUp();
		}

		@Override
		public void onClosed()
		{
			cleanUp();
		}

		private void cleanUp()
		{
			Log.i(TAG, String.format("Bridge to player %s is now closed", playerBridge.getPlayer().toString()));

			synchronized (bridgeLock)
			{
				connectedPlayerBridge = null;
				playerBridge.removeListener(this);
			}

			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					for (Listener l : listeners)
					{
						l.onPlayerQuit(playerBridge.getPlayer());
					}
				}
			});
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

			synchronized (activeHandshakeLock)
			{
				activeHandshake = null;
			}
		}

		@Override
		public void onError(HostHandshake sender, IOException error)
		{
			Log.e(TAG, String.format("Error occurred during handshake with client (%s)", error.toString()));

			synchronized (activeHandshakeLock)
			{
				activeHandshake = null;
			}
		}
	};

	private class QuitCallback implements PlayerBridge.NotifyCompleteCallback
	{
		private final PlayerBridge playerBridge;

		public QuitCallback(PlayerBridge playerBridge)
		{
			this.playerBridge = playerBridge;
		}

		@Override
		public void onCompleted(Exception error)
		{
			// Now the bridge can be closed
			playerBridge.close();
		}
	}

}
