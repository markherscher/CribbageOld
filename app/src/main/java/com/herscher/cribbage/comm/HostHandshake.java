package com.herscher.cribbage.comm;

import android.os.Handler;

import com.herscher.cribbage.Player;
import com.herscher.cribbage.comm.message.JoinGameRequestMessage;
import com.herscher.cribbage.comm.message.JoinGameResponseMessage;
import com.herscher.cribbage.comm.message.Message;

import java.io.IOException;

/**
 * TODO add comments
 */
public class HostHandshake
{
	private final static int RESPONSE_TIMEOUT = 2500;

	private final MessageConnection messageConnection;
	private final Listener listener;
	private final Handler handler;
	private JoinGameRequestMessage receivedRequest;
	private JoinGameResponseMessage outgoingResponse;
	private boolean isRunning;

	public HostHandshake(MessageConnection messageConnection, Listener listener, Handler handler)
	{
		if (messageConnection == null || listener == null || handler == null)
		{
			throw new IllegalArgumentException();
		}

		this.messageConnection = messageConnection;
		this.listener = listener;
		this.handler = handler;
	}

	public void start()
	{
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (!isRunning)
				{
					messageConnection.addListener(remoteConnectionListener);
					isRunning = true;
				}
			}
		});
	}

	public void stop()
	{
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				privateStop();
			}
		});
	}

	public MessageConnection getMessageConnection()
	{
		return messageConnection;
	}

	private void privateStop()
	{
		if (isRunning)
		{
			isRunning = false;
			handler.removeCallbacks(timeoutRunnable);
			messageConnection.removeListener(remoteConnectionListener);
			outgoingResponse = null;
			receivedRequest = null;
		}
	}

	private void handleRequestReceived(JoinGameRequestMessage requestMessage)
	{
		if (isRunning && outgoingResponse == null)
		{
			receivedRequest = requestMessage;
			outgoingResponse = new JoinGameResponseMessage(true, "success");
			messageConnection.send(outgoingResponse);
			handler.postDelayed(timeoutRunnable, RESPONSE_TIMEOUT);
		}
	}

	private void handleResponseSendComplete(Message message, IOException error)
	{
		if (isRunning && message == outgoingResponse)
		{
			if (error == null)
			{
				// Sent response acknowledged, so it's complete
				Player player = new Player(receivedRequest.getPlayerName(), receivedRequest.getPlayerId());
				privateStop();
				listener.onReady(this, player);
			}
			else
			{
				privateStop();
				listener.onError(this, error);
			}
		}
	}

	private Runnable timeoutRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			if (isRunning)
			{
				privateStop();
				listener.onTimedOut(HostHandshake.this);
			}
		}
	};

	private MessageConnection.Listener remoteConnectionListener = new MessageConnection.Listener()
	{
		@Override
		public void onSendComplete(final Message message, final IOException error)
		{
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					handleResponseSendComplete(message, error);
				}
			});
		}

		@Override
		public void onReceived(final Message message)
		{
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (message instanceof JoinGameRequestMessage)
					{
						handleRequestReceived((JoinGameRequestMessage) message);
					}
				}
			});
		}

		@Override
		public void onReceiveError(final IOException error)
		{
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (isRunning)
					{
						privateStop();
						listener.onError(HostHandshake.this, error);
					}
				}
			});
		}

		@Override
		public void onClosed()
		{
			stop();
		}
	};

	public interface Listener
	{
		void onReady(HostHandshake sender, Player player);

		void onTimedOut(HostHandshake sender);

		void onError(HostHandshake sender, IOException error);
	}
}