package com.herscher.cribbage.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.herscher.cribbage.Player;
import com.herscher.cribbage.R;

import java.lang.ref.WeakReference;

/**
 * TODO add comments
 */
public class LobbyFragment extends Fragment
{
	public interface Listener
	{
		void onStartClicked();

		void onCancelClicked();
	}

	private final static String TAG = "LobbyFragment";

	private WeakReference<Listener> listener;
	private TextView hostPlayerTextView;
	private TextView clientPlayerTextView;
	private Button startButton;
	private Button cancelButton;
	private String hostPlayerName;
	private String clientPlayerName;
	private boolean isHost;

	public LobbyFragment()
	{
		listener = new WeakReference<>(null);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.lobby_fragment, container, false);
	}

	@Override
	public void onViewCreated(View rootView, Bundle savedInstanceState)
	{
		hostPlayerTextView = (TextView) rootView.findViewById(R.id.hostPlayerTextView);
		clientPlayerTextView = (TextView) rootView.findViewById(R.id.clientPlayerTextView);
		startButton = (Button) rootView.findViewById(R.id.startButton);
		cancelButton = (Button) rootView.findViewById(R.id.cancelButton);

		cancelButton.setOnClickListener(clickListener);
		startButton.setOnClickListener(clickListener);
		startButton.setVisibility(isHost ? View.VISIBLE : View.INVISIBLE);

		showPlayerNames();
	}

	@Override
	public void onDetach()
	{
		if (startButton != null)
		{
			startButton.setOnClickListener(null);
		}

		if (cancelButton != null)
		{
			cancelButton.setOnClickListener(null);
		}

		hostPlayerTextView = null;
		clientPlayerTextView = null;
		startButton = null;
		cancelButton = null;

		super.onDetach();
	}

	@Override
	public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState)
	{
		super.onInflate(context, attrs, savedInstanceState);
		applyCustomAttributes(context, attrs);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState)
	{
		super.onInflate(activity, attrs, savedInstanceState);
		applyCustomAttributes(activity, attrs);
	}

	public void setListener(Listener listener)
	{
		this.listener = new WeakReference<>(listener);
	}

	public void setPlayers(Player host, Player client)
	{
		hostPlayerName = host.getName();
		clientPlayerName = client == null ? null : client.getName();
		showPlayerNames();
	}

	private void applyCustomAttributes(Context context, AttributeSet attrs)
	{
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LobbyFragment);
		isHost = a.getBoolean(R.styleable.LobbyFragment_is_host, true);
		a.recycle();
	}

	private void showPlayerNames()
	{
		if (hostPlayerTextView != null)
		{
			hostPlayerTextView.setText(hostPlayerName);
		}

		if (clientPlayerTextView != null)
		{
			clientPlayerTextView.setText(clientPlayerName == null ? "[open]" : clientPlayerName);
		}
	}

	private View.OnClickListener clickListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			Listener l = listener.get();

			if (l != null)
			{
				if (v == startButton)
				{
					l.onStartClicked();
				}
				else if (v == cancelButton)
				{
					l.onCancelClicked();
				}
			}
		}
	};
}
