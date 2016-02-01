package com.herscher.cribbage.comm.message;

/**
 * TODO add comments
 */
public class JoinGameResponseMessage extends Message
{
	private final boolean isAccepted;
	private final String description;
	// TODO: put CribbageGame instance here?

	public JoinGameResponseMessage(boolean isAccepted, String description)
	{
		if (description == null)
		{
			description = "";
		}

		this.isAccepted = isAccepted;
		this.description = description;
	}

	public boolean isAccepted()
	{
		return isAccepted;
	}

	public String getDescription()
	{
		return description;
	}

	@Override
	public String toString()
	{
		return String.format("JoinGameResponseMessage (%s)", isAccepted + "");
	}
}
