package com.herscher.cribbage.comm;

import com.herscher.cribbage.comm.message.Message;

import java.io.IOException;

/**
 * TODO add comments
 */
public interface MessageSerializer
{
	byte[] serialize(Message event) throws IOException;

	Message deserialize(byte[] bytes) throws IOException;
}
