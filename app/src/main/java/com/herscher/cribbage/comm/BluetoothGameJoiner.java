package com.herscher.cribbage.comm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.herscher.cribbage.Player;

import java.io.IOException;

/**
 * TODO add comments
 */
public class BluetoothGameJoiner
{
	private final static String TAG = "BluetoothGameJoiner";
	private final Handler handler;
	private Player player;
	private BluetoothDevice bluetoothDevice;
	private ConnectRunnable connectRunnable;
	private ClientHandshake clientHandshake;

	public BluetoothGameJoiner(BluetoothAdapter bluetoothAdapter, Handler handler)
	{
		this.handler = handler;
	}

	public synchronized void join(BluetoothDevice bluetoothDevice, Player localPlayer) throws IOException
	{
		if (bluetoothDevice == null || localPlayer == null)
		{
			throw new IllegalArgumentException();
		}

		if (connectRunnable != null)
		{
			throw new IllegalStateException("already joining");
		}

		this.bluetoothDevice = bluetoothDevice;
		player = localPlayer;
		BluetoothSocket bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(
				BluetoothConstants.RFCOMM_UUID);
	}

	public synchronized void cancelJoin()
	{
		if (connectRunnable != null)
		{
			connectRunnable.cancel();
			connectRunnable = null;
		}

		bluetoothDevice = null;
		clientHandshake = null;
		player = null;
	}

	public synchronized BluetoothDevice getBluetoothDevice()
	{
		return bluetoothDevice;
	}

	private void handleHostConnected(BluetoothSocket bluetoothSocket)
	{
		RemoteLink remoteLink;

		try
		{
			remoteLink = new BluetoothRemoteLink(bluetoothSocket);
		}
		catch (IOException e)
		{
			Log.e(TAG, String.format("Failed to create RemoteLink for host: %s", e.getMessage()));
			return;
		}

		MessageConnection messageConnection = new RemoteMessageConnection(
				new FrameRemoteTransport(remoteLink, handler), new KryoMessageSerializer());
		clientHandshake = new ClientHandshake(messageConnection, player, clientHandshakeListener, handler);
		clientHandshake.start();
	}

	private ClientHandshake.Listener clientHandshakeListener = new ClientHandshake.Listener()
	{
		@Override
		public void onReady()
		{

		}

		@Override
		public void onDenied(String reason)
		{

		}

		@Override
		public void onTimedOut()
		{

		}

		@Override
		public void onError(IOException error)
		{

		}
	};

	private class ConnectRunnable implements Runnable
	{
		private final BluetoothSocket bluetoothSocket;
		private boolean isRunning;

		public ConnectRunnable(BluetoothSocket bluetoothSocket)
		{
			this.bluetoothSocket = bluetoothSocket;
		}

		@Override
		public void run()
		{
			isRunning = true;

			try
			{
				bluetoothSocket.connect();
			}
			catch (IOException e)
			{
				if (isRunning)
				{
					Log.e(TAG, String.format("Failed to connect to host's bluetooth socket: %s", e.getMessage()));

				}
				cancel();
				return;
			}

			handleHostConnected(bluetoothSocket);
		}

		public void cancel()
		{
			isRunning = false;

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

	public interface Listener
	{

	}
}
