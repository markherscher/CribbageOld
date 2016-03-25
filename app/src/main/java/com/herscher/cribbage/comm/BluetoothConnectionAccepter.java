package com.herscher.cribbage.comm;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;

/**
 * Created by MarkHerscher on 3/7/2016.
 */
public class BluetoothConnectionAccepter
{
	private final Handler handler;

	public BluetoothConnectionAccepter(Handler handler)
	{
		if (handler == null)
		{
			throw new IllegalArgumentException();
		}

		this.handler = handler;
	}

	public RemoteMessageConnection acceptIncoming(BluetoothServerSocket serverSocket) throws IOException
	{
		if (serverSocket != null)
		{
			throw new IllegalStateException();
		}

		// accept() will block
		BluetoothSocket socket = serverSocket.accept();
		RemoteLink remoteLink = new BluetoothRemoteLink(socket);

		return new RemoteMessageConnection(new FrameRemoteTransport(remoteLink,
				handler), new KryoMessageSerializer());
	}
}
