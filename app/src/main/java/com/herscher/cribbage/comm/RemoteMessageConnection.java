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
	private final Queue<MessageCallbackPair> outgoingQueue;
	private final List<Listener> listeners;
	private MessageCallbackPair messagePairBeingSent;
	private boolean isOpen;
	private boolean closeWhenEmpty;

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
	public synchronized void send(Message message, MessageSendCallback callback)
	{
		if (message == null)
		{
			throw new IllegalArgumentException();
		}

		if (isOpen)
		{
			synchronized (outgoingQueue)
			{
				outgoingQueue.add(new MessageCallbackPair(message, callback));
				trySendNext();
			}
		}
		else if (callback != null)
		{
			callback.onSendComplete(message, new IOException("connection is closed"));
		}
	}

	@Override
	public boolean isOpen()
	{
		return isOpen;
	}

	@Override
	public synchronized void close()
	{
		if (isOpen)
		{
			isOpen = false;
			transport.close();

			// Clear all outstanding messages
			synchronized (outgoingQueue)
			{
				for (MessageCallbackPair pair : outgoingQueue)
				{
					if (pair.callback != null)
					{
						pair.callback.onSendComplete(pair.message, new IOException("connection is closed"));
					}
				}
			}

			for (Listener l : listeners)
			{
				l.onClosed();
			}

			outgoingQueue.clear();
			listeners.clear();
		}
	}

	public synchronized void setCloseWhenEmpty(boolean shouldClose)
	{
		if (isOpen)
		{
			closeWhenEmpty = shouldClose;

			if (closeWhenEmpty)
			{
				// Kick the queue to check it
				synchronized (outgoingQueue)
				{
					trySendNext();
				}
			}
		}
	}

	public boolean getCloseWhenEmpty()
	{
		return closeWhenEmpty;
	}

	private void trySendNext()
	{
		boolean isEmpty = false;

		if (messagePairBeingSent == null)
		{
			MessageCallbackPair next = outgoingQueue.poll();

			if (next != null)
			{
				byte[] rawBytes;

				try
				{
					rawBytes = messageSerializer.serialize(next.message);
				}
				catch (IOException e)
				{
					Log.w(TAG, String.format("Failed to serialize message %s: %s", next.message.toString(), e.getMessage()));
					trySendNext();
					return;
				}

				messagePairBeingSent = next;
				transport.startWrite(rawBytes);
			}
			else
			{
				isEmpty = true;
			}
		}

		if (isEmpty && closeWhenEmpty)
		{
			close();
		}
	}

	private static class MessageCallbackPair
	{
		private final Message message;
		private final MessageSendCallback callback;

		public MessageCallbackPair(Message message, MessageSendCallback callback)
		{
			this.message = message;
			this.callback = callback;
		}
	}

	private RemoteTransport.Listener transportListener = new RemoteTransport.Listener()
	{
		@Override
		public void onWriteComplete(IOException error)
		{
			if (isOpen)
			{
				MessageCallbackPair sentPair;

				synchronized (outgoingQueue)
				{
					sentPair = messagePairBeingSent;
					messagePairBeingSent = null;
					trySendNext();
				}

				if (sentPair != null && sentPair.callback != null)
				{
					sentPair.callback.onSendComplete(sentPair.message, error);
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
					Log.w(TAG, String.format("Failed to deserialize message: %s", e.getMessage()));
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
			close();
		}
	};
}
