package com.herscher.cribbage.comm.message;

/**
 * TODO add comments
 */
public class JoinGameRejectedResponseMessage extends Message
{
	private final String reason;

	public JoinGameRejectedResponseMessage(String reason)
	{
		if (reason == null)
		{
			reason = "";
		}

		this.reason = reason;
	}

	public String getReason()
	{
		return reason;
	}

	@Override
	public String toString()
	{
		return "JoinGameRejectedResponseMessage";
	}
}
