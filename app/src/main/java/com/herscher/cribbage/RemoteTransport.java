package com.herscher.cribbage;

import java.io.IOException;

/**
 * TODO add comments
 */
public interface RemoteTransport
{
	interface Listener
	{
		void onWriteComplete(IOException error);

		void onReceived(byte[] buffer);

		void onReadError(IOException error);

		void onClosed();
	}

	void addListener(Listener listener);

	void removeListener(Listener listener);

	void startWrite(final byte[] buffer);

	void close();
}
