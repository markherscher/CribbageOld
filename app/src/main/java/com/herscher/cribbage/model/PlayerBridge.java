package com.herscher.cribbage.model;

import com.herscher.cribbage.Card;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.RulesViolationException;
import com.herscher.cribbage.comm.message.Message;

/**
 * TODO add comments
 */
public interface PlayerBridge
{
	void addListener(Listener l);

	void removeListener(Listener l);

	void notifyCardsDiscarded(Card[] cards, NotifyCompleteCallback callback);

	void notifyCardsPlayed(Card card, NotifyCompleteCallback callback);

	void notifyRulesViolation(RulesViolationException error, NotifyCompleteCallback callback);

	void notifyQuit(NotifyCompleteCallback callback);

	void close();

	Player getPlayer();

	interface Listener
	{
		void onCardsDiscarded(Card[] cards);

		void onCardPlayed(Card card);

		void onRulesViolation(RulesViolationException error);

		void onQuit();

		void onClosed();
	}

	interface NotifyCompleteCallback
	{
		void onCompleted(Exception error);
	}
}
