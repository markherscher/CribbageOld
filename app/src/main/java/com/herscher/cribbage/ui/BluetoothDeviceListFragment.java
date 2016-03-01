package com.herscher.cribbage.ui;

import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.herscher.cribbage.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by MarkHerscher on 2/21/2016.
 */
public class BluetoothDeviceListFragment extends Fragment
{
	public interface Listener
	{
		void onDeviceSelected(BluetoothDevice device);
	}

	private final List<BluetoothDeviceWrapper> deviceList;
	private ListView gameListView;
	private ArrayAdapter<BluetoothDeviceWrapper> deviceListAdapter;
	private WeakReference<Listener> listener;

	public BluetoothDeviceListFragment()
	{
		deviceList = new ArrayList<>();
		listener = new WeakReference<>(null);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.bluetooth_device_list_fragment, container, false);
	}

	@Override
	public void onViewCreated(View rootView, Bundle savedInstanceState)
	{
		gameListView = (ListView) rootView.findViewById(R.id.gameListView);
		deviceListAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
				deviceList);

		gameListView.setAdapter(deviceListAdapter);
		gameListView.setOnItemClickListener(itemClickListener);
	}

	@Override
	public void onDetach()
	{
		super.onDetach();
		gameListView = null;
	}

	public void setListener(Listener listener)
	{
		this.listener = new WeakReference<>(listener);
	}

	public void addBluetoothDevice(BluetoothDevice bluetoothDevice)
	{
		deviceList.add(new BluetoothDeviceWrapper(bluetoothDevice));
		deviceListAdapter.notifyDataSetChanged();
	}

	public void clearBluetoothDevices()
	{
		deviceList.clear();
		deviceListAdapter.clear();
	}

	private static class BluetoothDeviceWrapper
	{
		public final BluetoothDevice bluetoothDevice;

		public BluetoothDeviceWrapper(BluetoothDevice bluetoothDevice)
		{
			this.bluetoothDevice = bluetoothDevice;
		}

		@Override
		public String toString()
		{
			return String.format("%s (%s)", bluetoothDevice.getName(),
					bluetoothDevice.getAddress());
		}
	}

	private ListView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			Listener l = listener.get();
			if (l != null && position < deviceList.size())
			{
				l.onDeviceSelected(deviceList.get(position).bluetoothDevice);
			}
		}
	};
}
