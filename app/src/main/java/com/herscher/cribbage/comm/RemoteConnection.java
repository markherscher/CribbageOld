package com.herscher.cribbage.comm;

import com.herscher.cribbage.model.GameEvent;

import java.io.IOException;

public interface RemoteConnection
{
	interface Listener
	{
		void onSendComplete(GameEvent event, IOException error);

		void onReceived(GameEvent event);

		void onReceiveError(IOException error);

		void onClosed();
	}

	void addListener(Listener listener);

	void removeListener(Listener listener);

	void send(GameEvent event);

	boolean isOpen();

	void close();
}
