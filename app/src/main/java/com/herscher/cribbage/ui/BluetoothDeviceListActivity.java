package com.herscher.cribbage.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.herscher.cribbage.R;
import com.herscher.cribbage.comm.BluetoothConstants;
import com.herscher.cribbage.comm.BluetoothRemoteLink;
import com.herscher.cribbage.comm.FrameRemoteTransport;
import com.herscher.cribbage.comm.KryoMessageSerializer;
import com.herscher.cribbage.comm.Lobby;
import com.herscher.cribbage.comm.LobbyJoiner;
import com.herscher.cribbage.comm.RemoteLink;
import com.herscher.cribbage.comm.RemoteMessageConnection;
import com.herscher.cribbage.model.LocalStuff;

import java.io.IOException;

/**
 * Created by MarkHerscher on 2/21/2016.
 */
public class BluetoothDeviceListActivity extends Activity
{
	private final static String CONNECTING_DIALOG_TAG = "connecting_progress_dialog";
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

		deviceListFragment.setListener(deviceListFragmentListener);
		refreshButton.setOnClickListener(clickListener);

		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(bluetoothBroadcastReceiver, filter);

		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering())
		{
			refreshButton.setEnabled(false);
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		deviceListFragment.setListener(null);
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

	private void showConnectingDialog()
	{
		ProgressDialogFragment dialogFragment = new ProgressDialogFragment();
		Bundle args = new Bundle();
		args.putString(ProgressDialogFragment.TITLE_ARG_KEY, "Connecting");
		args.putString(ProgressDialogFragment.MESSAGE_ARG_KEY, "Connecting to the game...");
		dialogFragment.setArguments(args);
		dialogFragment.show(getFragmentManager(), CONNECTING_DIALOG_TAG);
	}

	private void dismissConnectingDialog()
	{
		Fragment fragment = getFragmentManager().findFragmentByTag(CONNECTING_DIALOG_TAG);
		if (fragment != null)
		{
			((DialogFragment) fragment).dismiss();
		}
	}

	private void startBluetoothDiscovery()
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

	private void stopBluetoothDiscovery()
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
				startBluetoothDiscovery();
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

	private final BluetoothDeviceListFragment.Listener deviceListFragmentListener = new
			BluetoothDeviceListFragment.Listener()
			{
				@Override
				public void onDeviceSelected(BluetoothDevice device)
				{
					// TODO: NEED TO JOIN HERE
					stopBluetoothDiscovery();
					showConnectingDialog();

					new JoinThread(device, new Handler()).start();
				}
			};



	// TODO: move to fragment or service
	private class JoinThread extends Thread
	{
		private final BluetoothDevice device;
		private final Handler handler;

		public JoinThread(BluetoothDevice device, Handler handler)
		{
			this.device = device;
			this.handler = handler;
		}

		@Override
		public void run()
		{
			BluetoothSocket socket;

			try
			{
				socket = device.createInsecureRfcommSocketToServiceRecord(
						BluetoothConstants.RFCOMM_UUID);
			}
			catch (IOException e)
			{
				handleComplete(null, e);
				return;
			}

			try
			{
				socket.connect();
			}
			catch (IOException e)
			{
				handleComplete(null, e);
				return;
			}

			RemoteMessageConnection connection;
			try
			{
				connection = createRemoteMessageConnection(socket);
			}
			catch (IOException e)
			{
				handleComplete(null, e);
				return;
			}

			LobbyJoiner joiner = new LobbyJoiner(LocalStuff.localPlayer);
			Lobby lobby;

			try
			{
				lobby = joiner.join(connection);
			}
			catch (IOException e)
			{
				handleComplete(null, e);
				return;
			}

			handleComplete(lobby, null);
		}

		private RemoteMessageConnection createRemoteMessageConnection(BluetoothSocket socket) throws
				IOException
		{
			RemoteLink remoteLink = new BluetoothRemoteLink(socket);
			return new RemoteMessageConnection(new FrameRemoteTransport(remoteLink, handler), new KryoMessageSerializer());
		}

		private void handleComplete(final Lobby lobby, final IOException error)
		{
			final BluetoothDeviceListActivity activity = BluetoothDeviceListActivity.this;

			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					dismissConnectingDialog();

					if (error != null)
					{
						Toast.makeText(activity, "Failed", Toast.LENGTH_LONG).show();
						error.printStackTrace();
					}
					else if (lobby == null)
					{
						Toast.makeText(activity, "Timeout/denial", Toast.LENGTH_LONG).show();
					}
					else
					{
						Toast.makeText(activity, "Success", Toast.LENGTH_LONG).show();
					}
				}
			});
		}
	}
}
