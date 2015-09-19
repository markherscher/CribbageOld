package com.herscher.cribbage;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * TODO add comments
 *
 * Implement this for all scoring types, then add to StandardPlayScoringProcessor
 *
 */
public interface PlayScorer
{
	@Nullable
	ScoreUnit score(List<Card> cards);
}
