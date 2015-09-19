package com.herscher.cribbage;

import java.util.List;

/**
 * TODO add comments
 * TODO unimplement interface
 */
public interface ShowdownScoringProcessor
{
	ScoreUnit[] calculateScore(List<Card> cards, Card cutCard);
}
