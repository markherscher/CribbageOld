package com.herscher.cribbage.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import com.herscher.cribbage.R;

/**
 * Created by MarkHerscher on 2/21/2016.
 */
public class BluetoothDeviceListActivity extends Activity
{
	private BluetoothDeviceListFragment deviceListFragment;

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
	}

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

}
