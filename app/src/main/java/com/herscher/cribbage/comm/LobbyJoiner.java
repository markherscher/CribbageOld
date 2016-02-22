package com.herscher.cribbage.comm;

import android.os.Handler;
import android.util.Log;

import com.herscher.cribbage.Player;
import com.herscher.cribbage.comm.message.JoinGameAcceptedResponseMessage;
import com.herscher.cribbage.comm.message.JoinGameRejectedResponseMessage;
import com.herscher.cribbage.comm.message.JoinGameRequestMessage;
import com.herscher.cribbage.comm.message.Message;

import java.io.IOException;

/**
 * TODO add comments
 */
public class LobbyJoiner
{
	private final static int TOTAL_TIMEOUT = 5000;
	private final static String TAG = "LobbyJoiner";

	private final Object syncObject;
	private final Player player;
	private IOException exception;
	private Lobby lobby;
	private boolean isRunning;

	public LobbyJoiner(MessageConnection messageConnection, Player player, Handler handler)
	{
		if (messageConnection == null || player == null || handler == null)
		{
			throw new IllegalArgumentException();
		}

		this.player = player;
		syncObject = new Object();
	}

	public Lobby join(MessageConnection messageConnection) throws IOException
	{
		if (messageConnection == null)
		{
			throw new IllegalArgumentException();
		}

		synchronized (syncObject)
		{
			if (isRunning)
			{
				throw new IllegalStateException("already joining");
			}

			isRunning = true;
			exception = null;
			lobby = null;
			messageConnection.addListener(connectionListener);

			messageConnection.send(new JoinGameRequestMessage(0, player), new JoinRequestCallback());

			try
			{
				// Wait to receive a join game request. This timeout encompasses both the time
				// waiting for the join request and the time required to send the response. Ideally
				// it would be just the request timeout, but this is far easier and shouldn't have
				// adverse affects under most circumstances.
				syncObject.wait(TOTAL_TIMEOUT);
			}
			catch (InterruptedException e)
			{
				// That's fine. Ignore spurious interrupts here as the timeout isn't critical
			}

			isRunning = false;
			messageConnection.removeListener(connectionListener);

			if (lobby != null)
			{
				return lobby;
			}
			else if (exception == null)
			{
				// Timed out or denied
				return null;
			}
			else
			{
				throw exception;
			}
		}
	}

	public void cancelJoin()
	{
		synchronized (syncObject)
		{
			syncObject.notifyAll();
		}
	}

	private MessageConnection.Listener connectionListener = new MessageConnection.Listener()
	{
		@Override
		public void onReceived(final Message message)
		{
			synchronized (syncObject)
			{
				if (!isRunning)
				{
					return;
				}

				if (message instanceof JoinGameAcceptedResponseMessage)
				{
					Log.i(TAG, "Join request accepted");
					lobby = ((JoinGameAcceptedResponseMessage)message).getLobby();
					syncObject.notifyAll();
				}
				else if (message instanceof JoinGameRejectedResponseMessage)
				{
					Log.w(TAG, "Join request denied");
					syncObject.notifyAll();
				}
			}
		}

		@Override
		public void onReceiveError(final IOException error)
		{
			if (!isRunning)
			{
				return;
			}

			synchronized (syncObject)
			{
				Log.e(TAG, String.format("Error occurred during handshake with client (%s)", error.toString()));
				exception = error;
				syncObject.notifyAll();
			}
		}

		@Override
		public void onClosed()
		{
			Log.i(TAG, "Canceling due to MessageConnection closed");
			cancelJoin();
		}
	};

	private class JoinRequestCallback implements MessageConnection.MessageSendCallback
	{
		@Override
		public void onSendComplete(Message message, IOException error)
		{
			synchronized (syncObject)
			{
				if (!isRunning)
				{
					return;
				}

				// See if the request was delivered
				if (error != null)
				{
					Log.e(TAG, String.format("Error occurred sending join request (%s)", error.toString()));
					exception = error;
					syncObject.notifyAll();
				}
			}
		}
	}
}
