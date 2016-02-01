package com.herscher.cribbage.model;

import com.herscher.cribbage.Player;
import com.herscher.cribbage.RulesViolationException;
import com.herscher.cribbage.comm.message.Message;

/**
 * TODO add comments
 */
public class RulesViolationMessage extends Message
{
	private final RulesViolationException exception;
	private final Player player;

	public RulesViolationMessage(RulesViolationException exception, Player player)
	{
		if (exception == null || player == null)
		{
			throw new IllegalArgumentException();
		}

		this.exception = exception;
		this.player = player;
	}

	public RulesViolationException getException()
	{
		return exception;
	}

	public Player getPlayer()
	{
		return player;
	}
}
