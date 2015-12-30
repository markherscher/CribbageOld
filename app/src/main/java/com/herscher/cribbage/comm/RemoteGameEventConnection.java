package com.herscher.cribbage.comm;

import android.util.Log;

import com.herscher.cribbage.RemoteTransport;
import com.herscher.cribbage.model.GameEvent;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO add comments. And worry about thread safety?
 */
public class RemoteGameEventConnection implements RemoteConnection
{
	private final static String TAG = "RemoteGameEventConnection";

	private final RemoteTransport transport;
	private final GameEventSerializer gameEventSerializer;
	private final Queue<GameEvent> outgoingQueue;
	private final List<Listener> listeners;
	private GameEvent eventBeingSent;
	private boolean isOpen;

	public RemoteGameEventConnection(RemoteTransport transport, GameEventSerializer gameEventSerializer)
	{
		if (transport == null || gameEventSerializer == null)
		{
			throw new IllegalArgumentException();
		}

		this.transport = transport;
		this.gameEventSerializer = gameEventSerializer;
		outgoingQueue = new ConcurrentLinkedQueue<>();
		listeners = new CopyOnWriteArrayList<>();
		transport.addListener(transportListener);
		isOpen = true;
	}

	@Override
	public void addListener(Listener listener)
	{
		if (listener != null && !listeners.contains(listener))
		{
			listeners.add(listener);
		}
	}

	@Override
	public void removeListener(Listener listener)
	{
		listeners.remove(listener);
	}

	@Override
	public void send(GameEvent event)
	{
		if (event == null)
		{
			throw new IllegalArgumentException();
		}

		if (isOpen)
		{
			synchronized (outgoingQueue)
			{
				outgoingQueue.add(event);
				trySendNext();
			}
		}
		else
		{
			for (Listener l : listeners)
			{
				l.onSendComplete(event, new IOException("not open"));
			}
		}
	}

	@Override
	public boolean isOpen()
	{
		return isOpen;
	}

	@Override
	public void close()
	{
		if (isOpen)
		{
			isOpen = false;
			listeners.clear();
			transport.close();

			for (Listener l : listeners)
			{
				l.onClosed();
			}
		}
	}

	private void trySendNext()
	{
		if (eventBeingSent != null)
		{
			GameEvent event = outgoingQueue.poll();

			if (event != null)
			{
				byte[] rawBytes;

				try
				{
					rawBytes = gameEventSerializer.serialize(event);
				}
				catch (IOException e)
				{
					Log.w(TAG, String.format("Failed to serialize event %s: %s", event.toString(), e.getMessage()));
					trySendNext();
					return;
				}

				transport.startWrite(rawBytes);
			}
		}
	}

	private RemoteTransport.Listener transportListener = new RemoteTransport.Listener()
	{
		@Override
		public void onWriteComplete(IOException error)
		{
			if (isOpen)
			{
				GameEvent sentEvent;

				synchronized (outgoingQueue)
				{
					sentEvent = eventBeingSent;
					eventBeingSent = null;
					trySendNext();
				}

				for (Listener l : listeners)
				{
					l.onSendComplete(sentEvent, error);
				}
			}
		}

		@Override
		public void onReceived(byte[] buffer)
		{
			if (isOpen)
			{
				GameEvent receivedEvent;

				try
				{
					receivedEvent = gameEventSerializer.deserialize(buffer);
				}
				catch (IOException e)
				{
					Log.w(TAG, String.format("Failed to deserialize event: %s", e.getMessage()));
					return;
				}

				for (Listener l : listeners)
				{
					l.onReceived(receivedEvent);
				}
			}
		}

		@Override
		public void onReadError(IOException error)
		{
			if (isOpen)
			{
				for (Listener l : listeners)
				{
					l.onReceiveError(error);
				}
			}
		}

		@Override
		public void onClosed()
		{
			// Do nothing
		}
	};
}
