package com.herscher.cribbage.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.herscher.cribbage.Card;
import com.herscher.cribbage.CardCollection;
import com.herscher.cribbage.CardDeckFactory;
import com.herscher.cribbage.CribbageGame;
import com.herscher.cribbage.scoring.FifteensPlayScorer;
import com.herscher.cribbage.scoring.PairsPlayScorer;
import com.herscher.cribbage.scoring.PlayScoreProcessor;
import com.herscher.cribbage.scoring.PlayScorer;
import com.herscher.cribbage.Player;
import com.herscher.cribbage.R;
import com.herscher.cribbage.RulesViolationException;
import com.herscher.cribbage.scoring.RunsPlayScorer;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener
{
	private Button hostButton;
	private Button joinButton;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		hostButton = (Button) findViewById(R.id.hostButton);
		joinButton = (Button) findViewById(R.id.joinButton);

		hostButton.setOnClickListener(this);
		joinButton.setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.actionRestart)
		{
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v)
	{
		if (v == hostButton)
		{
			startActivity(new Intent(this, HostLobbyActivity.class));
		}
		else if (v == joinButton)
		{
			startActivity(new Intent(this, BluetoothDeviceListActivity.class));
		}
	}
}
