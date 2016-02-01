package com.herscher.cribbage.scoring;

import android.support.annotation.Nullable;

import com.herscher.cribbage.Card;

import java.util.List;

/**
 * TODO add comments
 *
 * Implement this for all scoring types, then add to PlayScoreProcessor
 *
 */
public interface PlayScorer
{
	@Nullable
	ScoreUnit score(List<Card> cards);
}
