package com.herscher.cribbage;

import android.test.AndroidTestCase;

import com.herscher.cribbage.comm.KryoMessageSerializer;
import com.herscher.cribbage.comm.Lobby;
import com.herscher.cribbage.comm.message.JoinGameAcceptedResponseMessage;
import com.herscher.cribbage.comm.message.Message;
import com.herscher.cribbage.scoring.FifteensPlayScorer;
import com.herscher.cribbage.scoring.PairsPlayScorer;
import com.herscher.cribbage.scoring.PlayScoreProcessor;
import com.herscher.cribbage.scoring.RunsPlayScorer;
import com.herscher.cribbage.scoring.ShowdownScoreProcessor;
import com.herscher.cribbage.scoring.StandardShowdownScoreProcessor;

import java.io.IOException;

/**
 * TODO add comments
 */
public class SomethingTest extends AndroidTestCase
{
	public void testFoo() throws IOException
	{
		KryoMessageSerializer serializer = new KryoMessageSerializer();
		PlayScoreProcessor playScoreProcessor = new PlayScoreProcessor(new FifteensPlayScorer(),
				new PairsPlayScorer(), new RunsPlayScorer());
		ShowdownScoreProcessor showdownScoreProcessor = new StandardShowdownScoreProcessor();

		CribbageGame game = new CribbageGame(playScoreProcessor, showdownScoreProcessor, 2);
		JoinGameAcceptedResponseMessage message = new JoinGameAcceptedResponseMessage(
				new Lobby(new Player("player 1", 320), new Player("player 2", 333), game));

		byte[] rawbytes = serializer.serialize(message);
		Message sMessage = serializer.deserialize(rawbytes);

		if (sMessage instanceof JoinGameAcceptedResponseMessage)
		{
			message = null;
		}
	}
}
