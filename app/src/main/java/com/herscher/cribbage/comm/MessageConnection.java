package com.herscher.cribbage.comm;

import com.herscher.cribbage.comm.message.Message;

import java.io.IOException;

public interface MessageConnection
{
	interface Listener
	{
		void onSendComplete(Message message, IOException error);

		void onReceived(Message message);

		void onReceiveError(IOException error);

		void onClosed();
	}

	void addListener(Listener listener);

	void removeListener(Listener listener);

	void send(Message message);

	boolean isOpen();

	void close();
}
