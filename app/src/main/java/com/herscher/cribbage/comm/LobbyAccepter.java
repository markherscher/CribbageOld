package com.herscher.cribbage.comm;

import android.util.Log;

import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.comm.message.JoinGameAcceptedResponseMessage;
import com.herscher.cribbage.comm.message.JoinGameRequestMessage;
import com.herscher.cribbage.comm.message.Message;

import java.io.IOException;

/**
 * TODO add comments.
 */
public class LobbyAccepter
{
	private final static String TAG = "LobbyAccepter";
	private final static int TOTAL_TIMEOUT = 5000;

	private final Player hostPlayer;
	private final CribbageGame game;
	private final Object syncObject;
	private MessageConnection messageConnection;
	private Message outgoingJoinGameResponse;
	private IOException exception;
	private Player joiningPlayer;
	private boolean isRunning;
	private boolean isAcceptSuccessful;

	public LobbyAccepter(Player hostPlayer, CribbageGame game)
	{
		if (hostPlayer == null || game == null)
		{
			throw new IllegalArgumentException();
		}

		this.hostPlayer = hostPlayer;
		this.game = game;
		syncObject = new Object();
	}

	public Player acceptConnection(MessageConnection messageConnection) throws IOException
	{
		if (messageConnection == null)
		{
			throw new IllegalArgumentException();
		}

		synchronized (syncObject)
		{
			if (isRunning)
			{
				throw new IllegalStateException("already accepting");
			}

			isRunning = true;
			isAcceptSuccessful = false;
			this.messageConnection = messageConnection;
			messageConnection.addListener(connectionListener);
			outgoingJoinGameResponse = null;
			joiningPlayer = null;
			exception = null;

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

			if (isAcceptSuccessful)
			{
				return joiningPlayer;
			}
			else if (exception == null)
			{
				// Timed out
				return null;
			}
			else
			{
				throw exception;
			}
		}
	}

	public void cancelAccept()
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
				if (message instanceof JoinGameRequestMessage)
				{

					if (!isRunning)
					{
						return;
					}

					// Respond to it
					if (outgoingJoinGameResponse == null)
					{
						joiningPlayer = ((JoinGameRequestMessage) message).getPlayer();
						outgoingJoinGameResponse = new JoinGameAcceptedResponseMessage(new Lobby(
								hostPlayer, joiningPlayer, game));
						messageConnection.send(outgoingJoinGameResponse, new JoinAcceptSendCallback());
					}
				}
			}
		}

		@Override
		public void onReceiveError(final IOException error)
		{
			synchronized (syncObject)
			{
				if (!isRunning)
				{
					return;
				}

				Log.e(TAG, String.format("Error occurred during handshake with client (%s)", error.toString()));
				exception = error;
				syncObject.notifyAll();
			}
		}

		@Override
		public void onClosed()
		{
			Log.i(TAG, "Canceling due to MessageConnection closed");
			cancelAccept();
		}
	};

	private class JoinAcceptSendCallback implements MessageConnection.MessageSendCallback
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

				// The outgoing response was delivered
				if (error == null)
				{
					Log.i(TAG, String.format("Player %s connection is ready", joiningPlayer));
					isAcceptSuccessful = true;
					syncObject.notifyAll();
				}
				else
				{
					Log.e(TAG, String.format("Error occurred sending join response (%s)", error.toString()));
					exception = error;
					syncObject.notifyAll();
				}
			}
		}
	}
}
