package com.herscher.cribbage.comm;

import android.os.Handler;
import android.util.Log;

import com.herscher.cribbage.Card;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.RulesViolationException;
import com.herscher.cribbage.comm.message.DiscardCardsMessage;
import com.herscher.cribbage.comm.message.Message;
import com.herscher.cribbage.comm.message.PlayCardMessage;
import com.herscher.cribbage.model.PlayerBridge;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO add comments
 */
public class RemotePlayerBridge implements PlayerBridge
{
	private final static String TAG = "RemotePlayerBridge";

	private final Player player;
	private final MessageConnection connection;
	private final Handler handler;
	private final HashMap<Message, NotifyCompleteCallback> sendCallbacks;
	private final List<Listener> listeners;
	private boolean isOpen;

	public RemotePlayerBridge(Player player, MessageConnection connection, Handler handler)
	{
		if (player == null || connection == null || handler == null)
		{
			throw new IllegalArgumentException();
		}

		this.player = player;
		this.connection = connection;
		this.handler = handler;
		sendCallbacks = new HashMap<>();
		listeners = new CopyOnWriteArrayList<>();
		connection.addListener(new RemoteConnectionListener());
	}

	@Override
	public void addListener(Listener l)
	{
		if (l != null && !listeners.contains(l))
		{
			listeners.add(l);
		}
	}

	@Override
	public void removeListener(Listener l)
	{
		listeners.remove(l);
	}

	@Override
	public void notifyCardsDiscarded(Card[] cards, NotifyCompleteCallback callback)
	{
		sendMessage(new DiscardCardsMessage(cards), callback);
	}

	@Override
	public void notifyCardsPlayed(Card card, NotifyCompleteCallback callback)
	{
		sendMessage(new PlayCardMessage(card), callback);
	}

	@Override
	public void notifyRulesViolation(RulesViolationException error, NotifyCompleteCallback callback)
	{
		// TODO
	}

	@Override
	public void close()
	{
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (isOpen)
				{
					isOpen = false;
					connection.close();
					listeners.clear();
					sendCallbacks.clear();

					for (Listener l : listeners)
					{
						l.onClosed();
					}
				}
			}
		});
	}

	@Override
	public Player getPlayer()
	{
		return player;
	}

	private void sendMessage(final Message message, final NotifyCompleteCallback callback)
	{
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (isOpen)
				{
					sendCallbacks.put(message, callback);
					connection.send(message);
				}
				else if (callback != null)
				{
					callback.onCompleted(new IOException("bridge is closed"));
				}
			}
		});
	}

	private void handleReceivedMessage(Message message)
	{
		if (message instanceof PlayCardMessage)
		{
			for (Listener l : listeners)
			{
				l.onCardPlayed(((PlayCardMessage) message).getCard());
			}
		}
		else if (message instanceof DiscardCardsMessage)
		{
			for (Listener l : listeners)
			{
				l.onCardsDiscarded(((DiscardCardsMessage) message).getCards());
			}
		}
	}

	private class RemoteConnectionListener implements MessageConnection.Listener
	{
		@Override
		public void onSendComplete(final Message event, final IOException error)
		{
			final NotifyCompleteCallback callback = sendCallbacks.remove(event);

			if (callback != null)
			{
				handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						if (isOpen)
						{
							callback.onCompleted(error);
						}
					}
				});
			}
		}

		@Override
		public void onReceived(final Message event)
		{
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (isOpen)
					{
						handleReceivedMessage(event);
					}
				}
			});
		}

		@Override
		public void onReceiveError(IOException error)
		{
			// TODO WHAT TO DO?
		}

		@Override
		public void onClosed()
		{
			Log.d(TAG, "Remote connection closed");
			close();
		}
	}
}
