package com.herscher.cribbage;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO add comments
 */
public final class CardDeckFactory
{
	public static List<Card> get52CardDeck()
	{
		List<Card> cardList = new ArrayList<>();

		cardList.clear();

		cardList.add(new Card(Card.Suit.CLUBS, Card.Face.ACE));
		cardList.add(new Card(Card.Suit.CLUBS, Card.Face.KING));
		cardList.add(new Card(Card.Suit.CLUBS, Card.Face.QUEEN));
		cardList.add(new Card(Card.Suit.CLUBS, Card.Face.JACK));
		cardList.add(new Card(Card.Suit.CLUBS, Card.Face.TEN));
		cardList.add(new Card(Card.Suit.CLUBS, Card.Face.NINE));
		cardList.add(new Card(Card.Suit.CLUBS, Card.Face.EIGHT));
		cardList.add(new Card(Card.Suit.CLUBS, Card.Face.SEVEN));
		cardList.add(new Card(Card.Suit.CLUBS, Card.Face.SIX));
		cardList.add(new Card(Card.Suit.CLUBS, Card.Face.FIVE));
		cardList.add(new Card(Card.Suit.CLUBS, Card.Face.FOUR));
		cardList.add(new Card(Card.Suit.CLUBS, Card.Face.THREE));
		cardList.add(new Card(Card.Suit.CLUBS, Card.Face.TWO));

		cardList.add(new Card(Card.Suit.DIAMONDS, Card.Face.ACE));
		cardList.add(new Card(Card.Suit.DIAMONDS, Card.Face.KING));
		cardList.add(new Card(Card.Suit.DIAMONDS, Card.Face.QUEEN));
		cardList.add(new Card(Card.Suit.DIAMONDS, Card.Face.JACK));
		cardList.add(new Card(Card.Suit.DIAMONDS, Card.Face.TEN));
		cardList.add(new Card(Card.Suit.DIAMONDS, Card.Face.NINE));
		cardList.add(new Card(Card.Suit.DIAMONDS, Card.Face.EIGHT));
		cardList.add(new Card(Card.Suit.DIAMONDS, Card.Face.SEVEN));
		cardList.add(new Card(Card.Suit.DIAMONDS, Card.Face.SIX));
		cardList.add(new Card(Card.Suit.DIAMONDS, Card.Face.FIVE));
		cardList.add(new Card(Card.Suit.DIAMONDS, Card.Face.FOUR));
		cardList.add(new Card(Card.Suit.DIAMONDS, Card.Face.THREE));
		cardList.add(new Card(Card.Suit.DIAMONDS, Card.Face.TWO));

		cardList.add(new Card(Card.Suit.HEARTS, Card.Face.ACE));
		cardList.add(new Card(Card.Suit.HEARTS, Card.Face.KING));
		cardList.add(new Card(Card.Suit.HEARTS, Card.Face.QUEEN));
		cardList.add(new Card(Card.Suit.HEARTS, Card.Face.JACK));
		cardList.add(new Card(Card.Suit.HEARTS, Card.Face.TEN));
		cardList.add(new Card(Card.Suit.HEARTS, Card.Face.NINE));
		cardList.add(new Card(Card.Suit.HEARTS, Card.Face.EIGHT));
		cardList.add(new Card(Card.Suit.HEARTS, Card.Face.SEVEN));
		cardList.add(new Card(Card.Suit.HEARTS, Card.Face.SIX));
		cardList.add(new Card(Card.Suit.HEARTS, Card.Face.FIVE));
		cardList.add(new Card(Card.Suit.HEARTS, Card.Face.FOUR));
		cardList.add(new Card(Card.Suit.HEARTS, Card.Face.THREE));
		cardList.add(new Card(Card.Suit.HEARTS, Card.Face.TWO));

		cardList.add(new Card(Card.Suit.SPADES, Card.Face.ACE));
		cardList.add(new Card(Card.Suit.SPADES, Card.Face.KING));
		cardList.add(new Card(Card.Suit.SPADES, Card.Face.QUEEN));
		cardList.add(new Card(Card.Suit.SPADES, Card.Face.JACK));
		cardList.add(new Card(Card.Suit.SPADES, Card.Face.TEN));
		cardList.add(new Card(Card.Suit.SPADES, Card.Face.NINE));
		cardList.add(new Card(Card.Suit.SPADES, Card.Face.EIGHT));
		cardList.add(new Card(Card.Suit.SPADES, Card.Face.SEVEN));
		cardList.add(new Card(Card.Suit.SPADES, Card.Face.SIX));
		cardList.add(new Card(Card.Suit.SPADES, Card.Face.FIVE));
		cardList.add(new Card(Card.Suit.SPADES, Card.Face.FOUR));
		cardList.add(new Card(Card.Suit.SPADES, Card.Face.THREE));
		cardList.add(new Card(Card.Suit.SPADES, Card.Face.TWO));

		return cardList;
	}
}
