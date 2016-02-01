package com.herscher.cribbage.comm;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO add comments
 *
 * Not thread safe
 */
public class BluetoothRemoteLink implements RemoteLink
{
	private final BluetoothSocket bluetoothSocket;
	private final InputStream inputStream;
	private final OutputStream outputStream;
	private boolean isOpen;

	public BluetoothRemoteLink(BluetoothSocket bluetoothSocket) throws IOException
	{
		if (bluetoothSocket == null)
		{
			throw new IllegalArgumentException();
		}

		this.bluetoothSocket = bluetoothSocket;
		inputStream = bluetoothSocket.getInputStream();
		outputStream = bluetoothSocket.getOutputStream();
		isOpen = true;
	}

	@Override
	public void write(byte[] bytes) throws IOException
	{
		outputStream.write(bytes);
	}

	@Override
	public byte[] read() throws IOException
	{
		byte[] readBuf = new byte[128];
		byte[] outBuf;
		int count = inputStream.read(readBuf);

		if (count <= 0)
		{
			outBuf = new byte[0];
		}
		else if (count == readBuf.length)
		{
			outBuf = readBuf;
		}
		else
		{
			outBuf = new byte[count];
			System.arraycopy(readBuf, 0, outBuf, 0, outBuf.length);
		}

		return outBuf;
	}

	@Override
	public void close()
	{
		if (isOpen)
		{
			try
			{
				inputStream.close();
			}
			catch (IOException e)
			{
				// Oh well
			}

			try
			{
				outputStream.close();
			}
			catch (IOException e)
			{
				// Oh well
			}

			try
			{
				bluetoothSocket.close();
			}
			catch (IOException e)
			{
				// Oh well
			}
		}
	}
}
