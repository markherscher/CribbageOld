package com.herscher.cribbage.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.herscher.cribbage.R;

/**
 * Created by MarkHerscher on 2/21/2016.
 */
public class BluetoothDeviceListActivity extends Activity
{
	private BluetoothDeviceListFragment deviceListFragment;
	private Button refreshButton;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bluetooth_device_list_activity);

		ActionBar actionBar = getActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		deviceListFragment = (BluetoothDeviceListFragment) getFragmentManager().findFragmentById(
				R.id.bluetoothDeviceListFragment);
		refreshButton = (Button) findViewById(R.id.refreshButton);

		refreshButton.setOnClickListener(clickListener);

		registerReceiver(bluetoothBroadcastReceiver,
				new IntentFilter(BluetoothDevice.ACTION_FOUND));
		registerReceiver(bluetoothBroadcastReceiver,
				new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		unregisterReceiver(bluetoothBroadcastReceiver);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		switch (id)
		{
			case android.R.id.home:
				finish();
				break;
		}

		//Intent myIntent = new Intent(getApplicationContext(), MyActivity.class);
		//startActivityForResult(myIntent, 0);
		return true;
	}

	private void startBluetoothScan()
	{
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
		{
			Toast.makeText(this, "You must enable Bluetooth", Toast.LENGTH_LONG).show();
		}
		else
		{
			bluetoothAdapter.cancelDiscovery();
			bluetoothAdapter.startDiscovery();
			deviceListFragment.clearBluetoothDevices();
		}
	}

	private void stopBluetoothScan()
	{
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (bluetoothAdapter != null)
		{
			bluetoothAdapter.cancelDiscovery();
		}
	}

	private final View.OnClickListener clickListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			if (v == refreshButton)
			{
				refreshButton.setEnabled(false);
				startBluetoothScan();
			}
		}
	};

	private final BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action))
			{
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if (deviceListFragment != null)
				{
					deviceListFragment.addBluetoothDevice(device);
				}
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
			{
				setProgressBarIndeterminateVisibility(false);
				refreshButton.setEnabled(true);
			}
		}
	};
}
