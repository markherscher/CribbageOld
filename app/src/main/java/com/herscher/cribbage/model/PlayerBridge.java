package com.herscher.cribbage.model;

import com.herscher.cribbage.Player;

/**
 * TODO add comments
 */
public interface PlayerBridge
{
	void addListener(Listener l);

	void removeListener(Listener l);

	void send(GameEvent event, GameEventSendCallback callback);

	void close();

	Player getPlayer();

	interface Listener
	{
		void onEventReceived(GameEvent event);

		void onClosed();
	}

	interface GameEventSendCallback
	{
		void onCompleted(GameEvent event, Exception error);
	}
}
