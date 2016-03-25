package com.herscher.cribbage.comm;

import android.os.Handler;

import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.comm.message.JoinGameAcceptedResponseMessage;
import com.herscher.cribbage.comm.message.JoinGameRejectedResponseMessage;
import com.herscher.cribbage.comm.message.JoinGameRequestMessage;
import com.herscher.cribbage.comm.message.Message;

import java.io.IOException;

/**
 * Created by MarkHerscher on 3/7/2016.
 */
public class HostHandshaker
{
	public interface Listener
	{
		void onJoinRequestReceived(HostHandshaker sender);

		void onIoException(HostHandshaker sender, IOException error);

		void onTimedOut(HostHandshaker sender);
	}

	private final static int JOIN_REQUEST_TIMEOUT = 2500;
	private final Object stateLock = new Object();
	private final RemoteMessageConnection messageConnection;
	private final Player localPlayer;
	private final CribbageGame game;
	private final Handler handler;
	private Listener listener;
	private boolean isWaitingForJoin;
	private boolean isAccepting;
	private Player joiningPlayer;

	public HostHandshaker(RemoteMessageConnection messageConnection, Player localPlayer,
						  CribbageGame game, Handler handler)
	{
		if (messageConnection == null || localPlayer == null || game == null || handler == null)
		{
			throw new IllegalArgumentException();
		}

		this.messageConnection = messageConnection;
		this.localPlayer = localPlayer;
		this.game = game;
		this.handler = handler;
	}

	public void waitForJoinRequest(Listener listener)
	{
		if (listener == null)
		{
			throw new IllegalArgumentException();
		}

		synchronized (stateLock)
		{
			if (isAccepting)
			{
				return;
			}
		}

		// Wait for the join request
		isAccepting = true;
		isWaitingForJoin = true;
		this.listener = listener;
		messageConnection.addListener(messageConnectionListener);
		handler.postDelayed(joinRequestTimeout, JOIN_REQUEST_TIMEOUT);
	}

	public void acceptJoinRequest()
	{
		synchronized (stateLock)
		{
			if (isAccepting && joiningPlayer != null)
			{
				Lobby lobby = new Lobby(localPlayer, joiningPlayer, game);
				messageConnection.send(new JoinGameAcceptedResponseMessage(lobby), null);
				cleanup();
			}
		}
	}

	// TODO: someone has to close the connection (outside this class)
	public void rejectJoinRequest(String reason)
	{
		synchronized (stateLock)
		{
			if (isAccepting && joiningPlayer != null)
			{
				messageConnection.send(new JoinGameRejectedResponseMessage(reason), null);
				cleanup();
			}
		}
	}

	public Player getJoiningPlayer()
	{
		return joiningPlayer;
	}

	public RemoteMessageConnection getMessageConnection()
	{
		return messageConnection;
	}

	private void cleanup()
	{
		isWaitingForJoin = false;
		isAccepting = false;
		listener = null;
		handler.removeCallbacks(joinRequestTimeout);
		messageConnection.removeListener(messageConnectionListener);
	}

	private final MessageConnection.Listener messageConnectionListener = new MessageConnection
			.Listener()
	{
		@Override
		public void onReceived(Message message)
		{
			if (message instanceof JoinGameRequestMessage)
			{
				synchronized (stateLock)
				{
					if (isWaitingForJoin)
					{
						isWaitingForJoin = false;
						handler.removeCallbacks(joinRequestTimeout);

						joiningPlayer = ((JoinGameRequestMessage) message).getPlayer();
						listener.onJoinRequestReceived(HostHandshaker.this);

						// Now it's up to the listener to call acceptJoin() or rejectJoin()
					}
				}
			}
		}

		@Override
		public void onReceiveError(IOException error)
		{
			synchronized (stateLock)
			{
				if (isAccepting)
				{
					cleanup();
					listener.onIoException(HostHandshaker.this, error);
				}
			}
		}

		@Override
		public void onClosed()
		{
			synchronized (stateLock)
			{
				if (isAccepting)
				{
					cleanup();
					listener.onIoException(HostHandshaker.this,
							new IOException("message connection was closed"));
				}
			}
		}
	};

	private final Runnable joinRequestTimeout = new Runnable()
	{
		@Override
		public void run()
		{
			synchronized (stateLock)
			{
				if (isAccepting)
				{
					cleanup();
					listener.onTimedOut(HostHandshaker.this);
				}
			}
		}
	};
}
