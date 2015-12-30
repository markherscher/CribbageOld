package com.herscher.cribbage.comm;

import java.io.IOException;

/**
 * TODO add comments
 */
public interface RemoteLink
{
	void write(byte[] bytes) throws IOException;

	byte[] read() throws IOException;

	void close();
}
