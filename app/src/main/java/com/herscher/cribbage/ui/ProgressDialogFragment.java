package com.herscher.cribbage.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

/**
 * Created by MarkHerscher on 2/28/2016.
 */
public class ProgressDialogFragment extends DialogFragment
{
	public final static String TITLE_ARG_KEY = "Title";
	public final static String MESSAGE_ARG_KEY = "Message";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setCancelable(false);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		Bundle arguments = getArguments();
		String title = "";
		String message = "";

		if (arguments != null)
		{
			title = arguments.getString(TITLE_ARG_KEY);
			message = arguments.getString(MESSAGE_ARG_KEY);
		}

		ProgressDialog dialog = new ProgressDialog(getActivity(), getTheme());
		dialog.setTitle(title == null ? "" : title);
		dialog.setMessage(message == null ? "" : message);
		dialog.setIndeterminate(true);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		return dialog;
	}
}
