package com.herscher.cribbage;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TODO add comments
 */
public class GameState
{
	public enum State
	{
		DISCARD,
		PLAY,
		SCORE,
		COMPLETED
	}

	private final Player[] players;
	private final CardCollection crib;
	private final List<Card> allCards;
	private CardCollection remainingDeck;
	private State state;
	private Player dealer;
	private Player activePlayer;
	private Card cutCard;

	public GameState(@NonNull Player[] players)
	{
		this.players = new Player[players.length];
		crib = new CardCollection();
		allCards = CardDeckFactory.get52CardDeck();
		state = State.DISCARD;

		System.arraycopy(players, 0, this.players, 0, players.length);
	}

	public Player[] getPlayers()
	{
		return players;
	}

	public void startNewRound()
	{
		// TODO: change active player
		state = State.DISCARD;

		for (Player p : players)
		{
			p.getDiscardedCards().clear();
			p.getHand().clear();
		}
	}

	public void discardCards(Player player, Card[] cards)
	{
		for (Player p : players)
		{
			if (p.equals(player))
			{
				List<Card> discardedCards = p.getDiscardedCards();

				for (Card c : cards)
				{
					discardedCards.add(c);
				}
				break;
			}
		}
	}

	public void playCards(Player player, Card card)
	{

	}
}
