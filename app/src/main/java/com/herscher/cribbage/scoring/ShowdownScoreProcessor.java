package com.herscher.cribbage.scoring;

import com.herscher.cribbage.Card;

import java.util.List;

/**
 * TODO add comments
 * TODO unimplement interface
 */
public interface ShowdownScoreProcessor
{
	ScoreUnit[] calculateScore(List<Card> cards, Card cutCard);
}
