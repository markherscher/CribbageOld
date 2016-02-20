package com.herscher.cribbage.comm;

import com.herscher.cribbage.comm.message.Message;

import java.io.IOException;

public interface MessageConnection
{
	interface Listener
	{
		void onReceived(Message message);

		void onReceiveError(IOException error);

		void onClosed();
	}

	interface MessageSendCallback
	{
		void onSendComplete(Message message, IOException error);
	}

	void addListener(Listener listener);

	void removeListener(Listener listener);

	void send(Message message, MessageSendCallback callback);

	void setCloseWhenEmpty(boolean shouldClose);

	boolean isOpen();

	void close();
}
