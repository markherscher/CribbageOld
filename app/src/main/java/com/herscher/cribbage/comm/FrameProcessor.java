package com.herscher.cribbage.comm;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

public class FrameProcessor
{
	private enum ReceiveState
	{
		LOOKING_FOR_START,
		PROCESSING_NORMAL,
		PROCESSING_ESCAPED
	}

	public final static int MAX_FRAME_DATA_LENGTH = 2048;

	private final static int CRC_LENGTH = 4;

	// frame type + frame ID + crc
	private final static int FRAME_PROTOCOL_LENGTH = 1 + 2 + CRC_LENGTH;
	private final static byte ESCAPE_BYTE = 0x7E;
	private final static byte ESCAPE_XOR = 0x20;
	private final static byte START_BYTE = 0x7C;
	private final static byte END_BYTE = 0x7D;

	private final ByteBuffer receiveBuffer;
	private ReceiveState receiveState;

	public static class Frame
	{
		private final byte[] data;
		private final byte frameType;
		private final short frameId;

		public Frame(byte frameType, short frameId, byte[] data)
		{
			if (data == null)
			{
				throw new IllegalArgumentException();
			}

			if (data.length > MAX_FRAME_DATA_LENGTH)
			{
				throw new IllegalArgumentException(String.format("Invalid byte length of %d",
						data.length));
			}

			this.data = data;
			this.frameType = frameType;
			this.frameId = frameId;
		}

		public byte[] getData()
		{
			return data;
		}

		public byte getFrameType()
		{
			return frameType;
		}

		public short getFrameId()
		{
			return frameId;
		}
	}

	/**
	 * Constructs a new {@code FrameProcessor}.
	 */
	public FrameProcessor()
	{
		receiveBuffer = ByteBuffer.allocate(MAX_FRAME_DATA_LENGTH);
		receiveState = ReceiveState.LOOKING_FOR_START;

		receiveBuffer.order(ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * This method is thread safe.
	 *
	 * @param frame
	 * @return
	 * @throws FrameFormatException
	 */
	public byte[] encode(Frame frame)
	{
		if (frame == null)
		{
			throw new IllegalArgumentException("frame is null");
		}

		// Count how many bytes we'll need, to avoid unnecessary allocation
		CRC32 crc = new CRC32();
		crc.update(frame.frameType);
		crc.update(frame.frameId & 0xFF);
		crc.update((frame.frameId >> 8) & 0xFF);
		crc.update(frame.data);

		int crcValue = (int) crc.getValue();
		int byteCount = countRequiredBytes(frame, crcValue);

		ByteBuffer outBuffer = ByteBuffer.allocate(byteCount);
		outBuffer.order(ByteOrder.LITTLE_ENDIAN);

		outBuffer.put(START_BYTE);

		// Add CRC
		for (int i = 0; i < CRC_LENGTH; i++)
		{
			encodeByte((byte) ((crcValue >> (8 * i)) & 0xFF), outBuffer);
		}

		// Add frame type and frame ID
		encodeByte(frame.frameType, outBuffer);
		encodeByte((byte) (frame.frameId & 0xFF), outBuffer);
		encodeByte((byte) ((frame.frameId >> 8) & 0xFF), outBuffer);

		// Add payload
		for (byte b : frame.data)
		{
			encodeByte(b, outBuffer);
		}

		outBuffer.put(END_BYTE);

		return outBuffer.array();
	}

	/**
	 * This method is not thread safe.
	 *
	 * @param b
	 * @return
	 * @throws FrameFormatException
	 */
	public Frame decode(byte b) throws FrameFormatException
	{
		try
		{
			switch (receiveState)
			{
				case LOOKING_FOR_START:
					handleLookingForStart(b);
					return null;

				case PROCESSING_ESCAPED:
					handleProcessingEscaped(b);
					return null;

				default:
				case PROCESSING_NORMAL:
					return handleProcessingNormal(b);
			}
		}
		catch (BufferOverflowException e)
		{
			// Too many bytes to be a valid frame
			resetDecodeState();
			throw new FrameFormatException("Too many bytes received without frame end");
		}
	}

	/**
	 * Resets the decoding state so all bytes passed to {@link #decode(byte)} are discarded.
	 */
	public void resetDecodeState()
	{
		resetDecodeState(ReceiveState.LOOKING_FOR_START);
	}

	private void resetDecodeState(ReceiveState newState)
	{
		receiveState = newState;
		receiveBuffer.position(0);
	}

	private void handleLookingForStart(byte b)
	{
		switch (b)
		{
			case START_BYTE:
				receiveState = ReceiveState.PROCESSING_NORMAL;
				break;

			default:
				// Ignore anything else
				break;
		}
	}

	private void handleProcessingEscaped(byte b) throws FrameFormatException
	{
		switch (b)
		{
			case START_BYTE:
				// Unexpected start byte, which means we must assume it was valid and start a new
				// frame
				resetDecodeState(ReceiveState.PROCESSING_NORMAL);
				throw new FrameFormatException("Received start byte when escaped byte expected");

			case ESCAPE_BYTE:
			case END_BYTE:
				// Error; discard all data received
				resetDecodeState();
				throw new FrameFormatException(
						String.format("Received special byte %d when escaped byte expected",
								b));

			default:
				// Unescape and add
				receiveState = ReceiveState.PROCESSING_NORMAL;
				receiveBuffer.put((byte) (b ^ ESCAPE_XOR));
				break;
		}
	}

	private Frame handleProcessingNormal(byte b) throws FrameFormatException
	{
		switch (b)
		{
			case START_BYTE:
				// Unexpected start byte, which means we must assume it was valid and start a new
				// frame
				resetDecodeState(ReceiveState.PROCESSING_NORMAL);
				throw new FrameFormatException("Received start byte when processing normally");

			case ESCAPE_BYTE:
				// Discard this byte and plan to unescape the next
				receiveState = ReceiveState.PROCESSING_ESCAPED;
				return null;

			case END_BYTE:
				// End of the frame
				try
				{
					return getFrameFromBuffer();
				}
				finally
				{
					receiveBuffer.position(0);
					receiveState = ReceiveState.LOOKING_FOR_START;
				}

			default:
				// Normal byte, so just add
				receiveBuffer.put(b);
				return null;
		}
	}

	private Frame getFrameFromBuffer() throws FrameFormatException
	{
		int actualBufferLength = receiveBuffer.position();

		if (actualBufferLength < FRAME_PROTOCOL_LENGTH)
		{
			// Not enough bytes
			throw new FrameFormatException(String.format("Not enough bytes to form frame (got %d)",
					actualBufferLength));
		}

		// Check CRC
		CRC32 crc = new CRC32();
		receiveBuffer.position(0);
		int receivedCrc = receiveBuffer.getInt();
		crc.update(receiveBuffer.array(), CRC_LENGTH, actualBufferLength - CRC_LENGTH);
		int calculatedCrc = (int) crc.getValue();

		if (calculatedCrc != receivedCrc)
		{
			throw new FrameFormatException(
					String.format("CRC failure; got 0x%04X calculated 0x%04X", receivedCrc,
							calculatedCrc));
		}

		// CRC passed validation
		byte frameType = receiveBuffer.get();
		short frameId = receiveBuffer.getShort();
		byte[] frameBytes = new byte[actualBufferLength - receiveBuffer.position()];
		System.arraycopy(receiveBuffer.array(), receiveBuffer.position(), frameBytes, 0, frameBytes.length);
		return new Frame(frameType, frameId, frameBytes);
	}

	private boolean mustEscapeByte(byte b)
	{
		switch (b)
		{
			case START_BYTE:
			case END_BYTE:
			case ESCAPE_BYTE:
				return true;

			default:
				return false;
		}
	}

	private void encodeByte(byte b, ByteBuffer buffer)
	{
		if (mustEscapeByte(b))
		{
			buffer.put(ESCAPE_BYTE);
			buffer.put((byte) (b ^ ESCAPE_XOR));
		}
		else
		{
			buffer.put(b);
		}
	}

	private int countRequiredBytes(Frame frame, int crc)
	{
		// +1 for start, +1 for end
		int byteCount = 1 + FRAME_PROTOCOL_LENGTH + frame.data.length + 1;

		// Frame type
		if (mustEscapeByte(frame.frameType))
		{
			byteCount++;
		}

		// Frame ID
		for (int i = 0; i < 2; i++)
		{
			if (mustEscapeByte((byte) ((frame.frameId >> (8 * i)) & 0xFF)))
			{
				byteCount++;
			}
		}

		// CRC
		for (int i = 0; i < CRC_LENGTH; i++)
		{
			if (mustEscapeByte((byte) ((crc >> (8 * i)) & 0xFF)))
			{
				byteCount++;
			}
		}

		// Frame data
		for (byte b : frame.data)
		{
			if (mustEscapeByte(b))
			{
				byteCount++;
			}
		}

		return byteCount;
	}
}

