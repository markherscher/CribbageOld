package com.herscher.cribbage.comm;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.herscher.cribbage.Card;
import com.herscher.cribbage.CardCollection;
import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.PlayerState;
import com.herscher.cribbage.comm.message.JoinGameAcceptedResponseMessage;
import com.herscher.cribbage.comm.message.JoinGameRejectedResponseMessage;
import com.herscher.cribbage.comm.message.JoinGameRequestMessage;
import com.herscher.cribbage.comm.message.Message;
import com.herscher.cribbage.scoring.FifteensPlayScorer;
import com.herscher.cribbage.scoring.PairsPlayScorer;
import com.herscher.cribbage.scoring.PlayScoreProcessor;
import com.herscher.cribbage.scoring.RunsPlayScorer;
import com.herscher.cribbage.scoring.StandardShowdownScoreProcessor;

import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * TODO add comments
 */
public class KryoMessageSerializer implements MessageSerializer
{
	private final Kryo kryo;

	public KryoMessageSerializer()
	{
		kryo = new Kryo();

		// Important to allow classes without default constructors to be created
		((Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy())
				.setFallbackInstantiatorStrategy(
						new StdInstantiatorStrategy());

		// Require registration so we crash if an unexpected class is serialized, as this is
		// preferable to failing deserialization on the other side because the random class ID is
		// different
		kryo.setRegistrationRequired(true);

		try
		{
			registerClasses();
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalStateException(
					String.format("kryo register failed (%s)", e.getMessage()));
		}
	}

	@Override
	public Message deserialize(byte[] rawBytes) throws IOException
	{
		if (rawBytes == null)
		{
			throw new IllegalArgumentException("rawBytes was null");
		}

		Object obj;
		Input input = new Input(rawBytes);

		try
		{
			obj = kryo.readClassAndObject(input);
		}
		catch (KryoException e)
		{
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
		finally
		{
			input.close();
		}

		if (obj instanceof Message)
		{
			return (Message) obj;
		}
		else
		{
			throw new IOException("resulting object type was not Message");
		}
	}

	@Override
	public byte[] serialize(Message message) throws IOException
	{
		if (message == null)
		{
			throw new IllegalArgumentException("message was null");
		}

		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		Output output = null;

		try
		{
			output = new Output(byteStream);
			kryo.writeClassAndObject(output, message);
		}
		catch (KryoException e)
		{
			throw new IOException(e.getMessage());
		}
		finally
		{
			if (output != null)
			{
				output.close();
			}
		}

		return byteStream.toByteArray();
	}

	private void registerClasses() throws ClassNotFoundException
	{
		int id = 25;

		kryo.register(Message.class, id++);
		kryo.register(Player.class, id++);
		kryo.register(Lobby.class, id++);
		kryo.register(JoinGameAcceptedResponseMessage.class, id++);
		kryo.register(JoinGameRejectedResponseMessage.class, id++);
		kryo.register(JoinGameRequestMessage.class, id++);
		kryo.register(CribbageGame.class, id++);
		kryo.register(CribbageGame.State.class, id++);
		kryo.register(PlayScoreProcessor.class, id++);
		kryo.register(StandardShowdownScoreProcessor.class, id++);
		kryo.register(CardCollection.class, id++);
		kryo.register(PlayerState.class, id++);
		kryo.register(ArrayList.class, id++);
		kryo.register(FifteensPlayScorer.class, id++);
		kryo.register(PairsPlayScorer.class, id++);
		kryo.register(RunsPlayScorer.class, id++);
		kryo.register(Class.forName("com.herscher.cribbage" +
				".CribbageGame$DiscardStateActionHandler"), id++);
		kryo.register(
				Class.forName("com.herscher.cribbage.CribbageGame$NewRoundStateActionHandler"),
				id++);
		kryo.register(Class.forName("com.herscher.cribbage.CribbageGame$PlayStateActionHandler"),
				id++);
		kryo.register(Class.forName("com.herscher.cribbage" +
				".CribbageGame$NewGameStateActionHandler"), id++);
		kryo.register(Card.class, id);
	}
}
