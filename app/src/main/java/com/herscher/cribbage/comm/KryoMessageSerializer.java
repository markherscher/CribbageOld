package com.herscher.cribbage.comm;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.herscher.cribbage.comm.message.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * TODO add comments
 */
public class KryoMessageSerializer implements MessageSerializer
{
	private final Kryo kryo;

	public KryoMessageSerializer()
	{
		kryo = new Kryo();

		kryo.register(Message.class);
	}

	@SuppressWarnings("rawtypes")
	public void registerClass(Class type)
	{
		kryo.register(type);
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
}
