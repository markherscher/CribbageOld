package com.herscher.cribbage;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * TODO add comments
 * This class is not thread safe.
 */
public class BluetoothDeviceScanner
{
	private final BluetoothAdapter adapter;
	private final Listener listener;
	private final Context context;
	private boolean isScanning;

	public BluetoothDeviceScanner(BluetoothAdapter adapter, Listener listener, Context context)
	{
		if (adapter == null || listener == null || context == null)
		{
			throw new IllegalArgumentException();
		}

		this.adapter = adapter;
		this.listener = listener;
		this.context = context;
	}

	public boolean startScanning()
	{
		if (!adapter.isEnabled())
		{
			return false;
		}

		stopScanning();

		context.registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		context.registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

		isScanning = true;
		adapter.startDiscovery();
		return true;
	}

	public void stopScanning()
	{
		if (adapter.isDiscovering())
		{
			adapter.cancelDiscovery();
		}

		if (isScanning)
		{
			isScanning = false;

			try
			{
				context.unregisterReceiver(bluetoothReceiver);
			}
			catch (IllegalArgumentException e)
			{
				// Oh wow it wasn't registered big deal who cares thanks Google
			}

			listener.onScanStopped();
		}
	}

	public boolean isScanning()
	{
		return isScanning;
	}

	private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();

			if (BluetoothDevice.ACTION_FOUND.equals(action))
			{
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				listener.onDeviceFound(device);
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
			{
				stopScanning();
			}
		}
	};

	public interface Listener
	{
		void onDeviceFound(BluetoothDevice device);

		void onScanStopped();
	}
}
