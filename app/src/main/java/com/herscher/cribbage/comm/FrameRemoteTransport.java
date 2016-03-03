package com.herscher.cribbage.comm;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO add comments
 */
public class FrameRemoteTransport implements RemoteTransport
{
	private final static String TAG = "FrameRemoteTransport";
	private final static byte DATA_FRAME_TYPE = (byte) 0xAA;
	private final static byte ACK_FRAME_TYPE = (byte) 0xBB;
	private final static int MAX_WRITE_ATTEMPTS = 3;
	private final static int WRITE_RETRY_TIMEOUT = 200;

	private final List<Listener> listeners;
	private final RemoteLink link;
	private final Handler handler;
	private final FrameProcessor frameProcessor;
	private final TimeoutRunnable timeoutRunnable;
	private WriteRunnable currentWrite;
	private boolean isOpen;
	private int lastSentFrameId;
	private int lastReceivedFrameId;

	public FrameRemoteTransport(RemoteLink link, Handler handler)
	{
		if (link == null || handler == null)
		{
			throw new IllegalArgumentException();
		}

		this.link = link;
		this.handler = handler;
		listeners = new CopyOnWriteArrayList<>();
		frameProcessor = new FrameProcessor();
		timeoutRunnable = new TimeoutRunnable();
		isOpen = true;
		lastSentFrameId = 0;
		lastReceivedFrameId = Integer.MIN_VALUE;
		new Thread(new ReadRunnable(), "FrameRemoteTransport.ReadRunnable").start();
	}

	@Override
	public void addListener(Listener listener)
	{
		if (listener != null && !listeners.contains(listener))
		{
			listeners.add(listener);
		}
	}

	@Override
	public void removeListener(Listener listener)
	{
		listeners.remove(listener);
	}

	@Override
	public void startWrite(final byte[] buffer)
	{
		if (buffer == null)
		{
			throw new IllegalArgumentException("buffer cannot be null");
		}

		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (!isOpen)
				{
					// Don't use handleWriteComplete() as that will modify the current write
					for (Listener l : listeners)
					{
						l.onWriteComplete(new IOException("not open"));
					}
				}
				else if (currentWrite != null)
				{
					// Don't use handleWriteComplete() as that will modify the current write
					for (Listener l : listeners)
					{
						l.onWriteComplete(new IOException("write already in progress"));
					}
				}
				else
				{
					// When incrementing keep unsigned
					lastSentFrameId = (lastSentFrameId + 1) & 0xFFFF;
					currentWrite = new WriteRunnable(buffer, (short) lastSentFrameId);
					currentWrite.performWrite();
				}
			}
		});
	}

	@Override
	public void close()
	{
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (isOpen)
				{
					isOpen = false;
					listeners.clear();
					link.close();
					for (Listener l : listeners)
					{
						l.onClosed();
					}
				}
			}
		});
	}

	private void handleWriteComplete(final IOException e)
	{
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (currentWrite != null && isOpen)
				{
					currentWrite = null;
					timeoutRunnable.stop();
					for (Listener l : listeners)
					{
						l.onWriteComplete(e);
					}
				}
			}
		});
	}

	private void handleFrameReceived(FrameProcessor.Frame frame)
	{
		switch (frame.getFrameType())
		{
			case ACK_FRAME_TYPE:
				handleAckFrameReceived(frame);
				break;

			case DATA_FRAME_TYPE:
				handleDataFrameReceived(frame);
				break;

			default:
				Log.w(TAG, String.format("Frame of unknown type (%d) received",
						frame.getFrameType()));
				break;
		}
	}

	private void handleAckFrameReceived(final FrameProcessor.Frame frame)
	{
		Log.v(TAG, String.format("Received ack frame for id %d", frame.getFrameId()));

		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (currentWrite != null && frame.getFrameId() == currentWrite.frame.getFrameId())
				{
					// Received ACK
					handleWriteComplete(null);
				}
			}
		});
	}

	private void handleDataFrameReceived(final FrameProcessor.Frame frame)
	{
		// Always send acknowledgement. Otherwise, if an ACK is dropped and the data is
		// retried the sender will not be informed their second try was also received, and
		// consider it a failure.
		writeAckForFrame(frame);

		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				// New frame's ID must be greater than the last received ID. Use unsigned.
				int receivedFrameId = (int) (frame.getFrameId()) & 0xFFFF;
				if (receivedFrameId > lastReceivedFrameId)
				{
					Log.v(TAG, String.format("Received data frame with id %d", receivedFrameId));

					lastReceivedFrameId = receivedFrameId;
					for (Listener l : listeners)
					{
						l.onReceived(frame.getData());
					}
				}
				else
				{
					Log.w(TAG, String.format("Ignored received data frame with ID %d",
							receivedFrameId));
				}
			}
		});
	}

	private void writeAckForFrame(FrameProcessor.Frame frame)
	{
		final FrameProcessor.Frame ackFrame = new FrameProcessor.Frame(ACK_FRAME_TYPE,
				frame.getFrameId(), new byte[0]);

		AsyncTask.execute(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Log.v(TAG, String.format("Writing ack frame for id %d", ackFrame.getFrameId
							()));
					writeToLink(frameProcessor.encode(ackFrame));
				}
				catch (IOException e)
				{
					// Don't call the listener for write completed, as that's only for data writes
					// originating from here
					Log.e(TAG, String.format("IOException writing ACK (%s)", e.getMessage()));
				}
			}
		});
	}

	private synchronized void writeToLink(byte[] bytes) throws IOException
	{
		link.write(bytes);
	}

	private class WriteRunnable implements Runnable
	{
		private final byte[] frameBytes;
		private final FrameProcessor.Frame frame;
		public int writeCount;

		public WriteRunnable(byte[] buffer, short frameId)
		{
			frame = new FrameProcessor.Frame(DATA_FRAME_TYPE, frameId, buffer);
			frameBytes = frameProcessor.encode(frame);
		}

		@Override
		public void run()
		{
			// Reminder: runs on AsyncTask worker thread
			try
			{
				Log.v(TAG, String.format("Writing data frame with id %d", frame.getFrameId()));
				writeToLink(frameBytes);
			}
			catch (final IOException e)
			{
				handleWriteComplete(e);
			}
		}

		public void performWrite()
		{
			writeCount++;
			timeoutRunnable.start();
			AsyncTask.execute(this);
		}
	}

	private class ReadRunnable implements Runnable
	{
		@Override
		public void run()
		{
			while (isOpen)
			{
				byte[] rxBytes = null;

				try
				{
					rxBytes = link.read();
				}
				catch (final IOException e)
				{
					handler.post(new Runnable()
					{
						@Override
						public void run()
						{
							if (isOpen)
							{
								for (Listener l : listeners)
								{
									l.onReadError(e);
								}
							}
						}
					});
				}

				if (rxBytes != null)
				{
					handleReceivedBytes(rxBytes);
				}
			}
		}

		private void handleReceivedBytes(byte[] rxBytes)
		{
			for (byte b : rxBytes)
			{
				FrameProcessor.Frame frame = null;

				try
				{
					frame = frameProcessor.decode(b);
				}
				catch (FrameFormatException e)
				{
					Log.e(TAG, String.format("Received invalid frame (%s)", e.getMessage()));
				}

				if (frame != null)
				{
					handleFrameReceived(frame);
				}
			}
		}
	}

	private class TimeoutRunnable implements Runnable
	{
		@Override
		public void run()
		{
			if (!isOpen || currentWrite == null)
			{
				return;
			}

			if (currentWrite.writeCount >= MAX_WRITE_ATTEMPTS)
			{
				// Give up trying to send
				Log.e(TAG, String.format("No ack received for data frame id %d, giving up",
						currentWrite.frame.getFrameId()));
				handleWriteComplete(new IOException("frame not acknowledged"));
			}
			else
			{
				// Try again
				Log.w(TAG, String.format("No ack received for data frame id %d, trying again",
						currentWrite.frame.getFrameId()));
				currentWrite.performWrite();
			}
		}

		public void start()
		{
			stop();
			handler.postDelayed(this, WRITE_RETRY_TIMEOUT);
		}

		public void stop()
		{
			handler.removeCallbacks(this);
		}
	}
}
