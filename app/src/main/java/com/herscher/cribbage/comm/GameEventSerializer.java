package com.herscher.cribbage.comm;

import com.herscher.cribbage.model.GameEvent;

import java.io.IOException;

/**
 * TODO add comments
 */
public interface GameEventSerializer
{
	byte[] serialize(GameEvent event) throws IOException;

	GameEvent deserialize(byte[] bytes) throws IOException;
}
