package com.herscher.cribbage.comm;

import android.util.Log;

import com.herscher.cribbage.comm.message.Message;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO add comments. And worry about thread safety?
 */
public class RemoteMessageConnection implements MessageConnection
{
	private final static String TAG = "RemoteGameEventConn";

	private final RemoteTransport transport;
	private final MessageSerializer messageSerializer;
	private final Queue<Message> outgoingQueue;
	private final List<Listener> listeners;
	private Message eventBeingSent;
	private boolean isOpen;

	public RemoteMessageConnection(RemoteTransport transport, MessageSerializer messageSerializer)
	{
		if (transport == null || messageSerializer == null)
		{
			throw new IllegalArgumentException();
		}

		this.transport = transport;
		this.messageSerializer = messageSerializer;
		outgoingQueue = new ConcurrentLinkedQueue<>();
		listeners = new CopyOnWriteArrayList<>();
		this.transport.addListener(transportListener);
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
	public void send(Message event)
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
			Message event = outgoingQueue.poll();

			if (event != null)
			{
				byte[] rawBytes;

				try
				{
					rawBytes = messageSerializer.serialize(event);
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
				Message sentEvent;

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
				Message receivedEvent;

				try
				{
					receivedEvent = messageSerializer.deserialize(buffer);
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
