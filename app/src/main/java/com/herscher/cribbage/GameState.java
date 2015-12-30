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

	private final List<Player> players;
	private final CardCollection crib;
	private final List<Card> allCards;
	private CardCollection remainingDeck;
	private State state;
	private Player dealer;
	private Player activePlayer;
	private Card cutCard;

	public GameState(@NonNull Player[] players)
	{
		this.players = new ArrayList<>();
		crib = new CardCollection();
		allCards = CardDeckFactory.get52CardDeck();
		state = State.DISCARD;

		for (Player p : players)
		{
			this.players.add(p);
		}
	}

	public List<Player> getPlayers()
	{
		return players;
	}

	public CardCollection getCrib()
	{
		return crib;
	}

	public List<Card> getAllCards()
	{
		return allCards;
	}

	public CardCollection getRemainingDeck()
	{
		return remainingDeck;
	}

	public void setRemainingDeck(CardCollection remainingDeck)
	{
		this.remainingDeck = remainingDeck;
	}

	public State getState()
	{
		return state;
	}

	public void setState(State state)
	{
		this.state = state;
	}

	public Player getDealer()
	{
		return dealer;
	}

	public void setDealer(Player dealer)
	{
		this.dealer = dealer;
	}

	public Player getActivePlayer()
	{
		return activePlayer;
	}

	public void setActivePlayer(Player activePlayer)
	{
		this.activePlayer = activePlayer;
	}

	public Card getCutCard()
	{
		return cutCard;
	}

	public void setCutCard(Card cutCard)
	{
		this.cutCard = cutCard;
	}
}
