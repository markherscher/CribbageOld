package com.herscher.cribbage.scoring;

import com.herscher.cribbage.Card;
import com.herscher.cribbage.ScoreUnit;

import java.util.List;

/**
 * TODO add comments
 * TODO unimplement interface
 */
public interface ShowdownScoreProcessor
{
	ScoreUnit[] calculateScore(List<Card> cards, Card cutCard);
}
