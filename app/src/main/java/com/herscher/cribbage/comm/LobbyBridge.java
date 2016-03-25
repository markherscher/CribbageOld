package com.herscher.cribbage.comm;

import com.herscher.cribbage.Player;
import com.herscher.cribbage.comm.message.Message;
import com.herscher.cribbage.comm.message.PlayerQuitMessage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by MarkHerscher on 3/7/2016.
 */
public class LobbyBridge
{
	public interface Listener
	{
		void onPlayerQuit(LobbyBridge sender);

		void onGameStart();

		void onReceiveError(LobbyBridge sender, Exception error);

		void onSendError(LobbyBridge sender, Exception error);
	}

	private final Player player;
	private final MessageConnection connection;
	private final List<Listener> listeners;

	public LobbyBridge(Player player, MessageConnection connection)
	{
		if (player == null || connection == null)
		{
			throw new IllegalArgumentException();
		}

		this.player = player;
		this.connection = connection;
		listeners = new CopyOnWriteArrayList<>();

		connection.addListener(messageConnectionListener);
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

	/**
	 * Sends a message to the bridge's player indicating the specified player has quit.
	 *
	 * @param quittingPlayer
	 */
	public void sendPlayerQuit(Player quittingPlayer)
	{
		connection.send(new PlayerQuitMessage(quittingPlayer),
				new MessageConnection.MessageSendCallback()
				{
					@Override
					public void onSendComplete(Message message, IOException error)
					{
						if (error != null)
						{
							for (Listener l : listeners)
							{
								l.onSendError(LobbyBridge.this, error);
							}
						}
					}
				});
	}

	public void startGame()
	{

	}

	public void close()
	{
		disconnect();
	}

	private void handlePlayerQuit()
	{
		disconnect();

		for (Listener l : listeners)
		{
			l.onPlayerQuit(this);
		}
	}

	private void handleReceiveError(Exception error)
	{
		disconnect();

		for (Listener l : listeners)
		{
			l.onReceiveError(this, error);
		}
	}

	private void disconnect()
	{
		connection.removeListener(messageConnectionListener);
		connection.close();
	}

	private MessageConnection.Listener messageConnectionListener = new MessageConnection.Listener()
	{
		@Override
		public void onReceived(Message message)
		{
			if (message instanceof PlayerQuitMessage)
			{
				if (((PlayerQuitMessage) message).getPlayer().equals(player))
				{
					handlePlayerQuit();
				}
			}
		}

		@Override
		public void onReceiveError(IOException error)
		{
			handleReceiveError(error);
		}

		@Override
		public void onClosed()
		{
			disconnect();
		}
	};
}
