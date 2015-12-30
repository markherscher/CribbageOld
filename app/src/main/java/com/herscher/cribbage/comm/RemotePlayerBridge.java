package com.herscher.cribbage.comm;

import android.os.Handler;
import android.util.Log;

import com.herscher.cribbage.Player;
import com.herscher.cribbage.model.GameEvent;
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
	private final RemoteConnection connection;
	private final Handler handler;
	private final HashMap<GameEvent, GameEventSendCallback> sendCallbacks;
	private final List<Listener> listeners;
	private boolean isOpen;

	public RemotePlayerBridge(Player player, RemoteConnection connection, Handler handler)
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
	public void send(final GameEvent event, final GameEventSendCallback callback)
	{
		if (event == null)
		{
			throw new IllegalArgumentException();
		}

		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (isOpen)
				{
					sendCallbacks.put(event, callback);
					connection.send(event);
				}
				else if (callback != null)
				{
					callback.onCompleted(event, new IOException("bridge is closed"));
				}
			}
		});
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

	private class RemoteConnectionListener implements RemoteConnection.Listener
	{
		@Override
		public void onSendComplete(final GameEvent event, final IOException error)
		{
			final GameEventSendCallback callback = sendCallbacks.remove(event);

			if (callback != null)
			{
				handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						if (isOpen)
						{
							callback.onCompleted(event, error);
						}
					}
				});
			}
		}

		@Override
		public void onReceived(final GameEvent event)
		{
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (isOpen)
					{
						for (Listener l : listeners)
						{
							l.onEventReceived(event);
						}
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
