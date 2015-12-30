package com.herscher.cribbage.comm;

/**
 * TODO add comments
 */
public class FrameFormatException extends Exception
{
	public FrameFormatException(String msg)
	{
		super(msg);
	}

	public FrameFormatException(String msg, Exception cause)
	{
		super(msg, cause);
	}
}
