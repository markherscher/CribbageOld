package com.herscher.cribbage.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.herscher.cribbage.R;

/**
 * Created by MarkHerscher on 2/21/2016.
 */
public class BluetoothDeviceListFragment extends Fragment
{
	private ListView gameListView;

	public BluetoothDeviceListFragment()
	{
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.bluetooth_device_list_fragment, container, false);
	}

	@Override
	public void onViewCreated(View rootView, Bundle savedInstanceState)
	{
		gameListView = (ListView) rootView.findViewById(R.id.gameListView);
	}

	@Override
	public void onDetach()
	{
		gameListView = null;
		super.onDetach();
	}
}
