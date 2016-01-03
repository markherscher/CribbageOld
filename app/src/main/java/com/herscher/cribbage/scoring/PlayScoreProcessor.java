package com.herscher.cribbage.scoring;

import android.support.annotation.NonNull;

import com.herscher.cribbage.Card;
import com.herscher.cribbage.RulesViolationException;
import com.herscher.cribbage.ScoreUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO add comments
 *
 * PlayScoringProcessor
 * PlayScorer
 *
 */
public class PlayScoreProcessor
{
	private final static int MAX_COUNT = 31;

	private final List<Card> playedCards;
	private final PlayScorer[] scorers;
	private int count;

	public PlayScoreProcessor(PlayScorer... scorers)
	{
		this.scorers = scorers;
		playedCards = new ArrayList<>();
	}

	public ScoreUnit[] playCard(@NonNull Card card) throws RulesViolationException
	{
		if (!isCardLegalToPlay(card))
		{
			throw new RulesViolationException("card is not legal to play");
		}

		playedCards.add(card);
		count += getValue(card);

		List<ScoreUnit> scores = new ArrayList<>();

		for (PlayScorer ps : scorers)
		{
			ScoreUnit scoreUnit = ps.score(playedCards);

			if (scoreUnit != null)
			{
				scores.add(scoreUnit);
			}
		}

		return scores.toArray(new ScoreUnit[scores.size()]);
	}

	public boolean isCardLegalToPlay(@NonNull Card card)
	{
		return getValue(card) + count <= MAX_COUNT;
	}

	public int getCount()
	{
		return count;
	}

	public void reset()
	{
		count = 0;
		playedCards.clear();
	}


	private int getValue(Card card)
	{
		switch (card.getFace())
		{
			case ACE:
				return 1;
			case TWO:
				return 2;
			case THREE:
				return 3;
			case FOUR:
				return 4;
			case FIVE:
				return 5;
			case SIX:
				return 6;
			case SEVEN:
				return 7;
			case EIGHT:
				return 8;
			case NINE:
				return 9;
			case TEN:
			case JACK:
			case QUEEN:
			case KING:
				return 10;
			default:
				return 0;
		}
	}
}
