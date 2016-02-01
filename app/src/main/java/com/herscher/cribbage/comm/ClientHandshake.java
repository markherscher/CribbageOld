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
public class ClientHandshake
{
	private final static int RESPONSE_TIMEOUT = 2500;

	private final MessageConnection messageConnection;
	private final Listener listener;
	private final Player player;
	private final Handler handler;
	private boolean isRunning;
	private JoinGameRequestMessage outgoingRequest;

	public ClientHandshake(MessageConnection messageConnection, Player player, Listener listener, Handler handler)
	{
		if (messageConnection == null || player == null || listener == null || handler == null)
		{
			throw new IllegalArgumentException();
		}

		this.messageConnection = messageConnection;
		this.player = player;
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
					messageConnection.addListener(eventConnectionListener);
					isRunning = true;
					outgoingRequest = new JoinGameRequestMessage(1, player.getName(), player.getId());
					messageConnection.send(outgoingRequest);
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
			messageConnection.removeListener(eventConnectionListener);
			handler.removeCallbacks(timeoutRunnable);
			outgoingRequest = null;
		}
	}

	private void handleRequestSendComplete(Message message, IOException error)
	{
		if (isRunning && message == outgoingRequest)
		{
			if (error == null)
			{
				// Join request sent successfully, now start timer for response
				handler.postDelayed(timeoutRunnable, RESPONSE_TIMEOUT);
			}
			else
			{

			}
		}
	}

	private void handleResponseReceived(JoinGameResponseMessage responseMessage)
	{
		if (isRunning)
		{
			privateStop();

			if (responseMessage.isAccepted())
			{
				listener.onReady();
			}
			else
			{
				listener.onDenied(responseMessage.getDescription());
			}
		}
	}

	private MessageConnection.Listener eventConnectionListener = new MessageConnection.Listener()
	{
		@Override
		public void onSendComplete(final Message message, final IOException error)
		{
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					handleRequestSendComplete(message, error);
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
					if (message instanceof JoinGameResponseMessage)
					{
						handleResponseReceived((JoinGameResponseMessage) message);
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
						listener.onError(error);
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

	private Runnable timeoutRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			if (isRunning)
			{
				privateStop();
				listener.onTimedOut();
			}
		}
	};

	public interface Listener
	{
		void onReady();

		void onDenied(String reason);

		void onTimedOut();

		void onError(IOException error);
	}
}
