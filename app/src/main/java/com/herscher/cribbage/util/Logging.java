package com.herscher.cribbage.util;

import android.util.Log;

/**
 * TODO add comments
 */
public class Logging
{
	public static final int LEVEL_NONE = 0;
	public static final int LEVEL_VERBOSE = 1;
	public static final int LEVEL_DEBUG = 2;
	public static final int LEVEL_INFO = 3;
	public static final int LEVEL_WARNING = 4;
	public static final int LEVEL_ERROR = 5;

	private final static String TAG = "Cribbage_";
	private static int logLevel = LEVEL_ERROR;

	public static void setLogLevel(int level)
	{
		logLevel = level;
	}

	public static void e(String tag, String msg)
	{
		if (logLevel >= LEVEL_ERROR)
		{
			Log.e(TAG + tag, msg);
		}
	}

	public static void w(String tag, String msg)
	{
		if (logLevel >= LEVEL_WARNING)
		{
			Log.w(TAG + tag, msg);
		}
	}

	public static void i(String tag, String msg)
	{
		if (logLevel >= LEVEL_INFO)
		{
			Log.i(TAG + tag, msg);
		}
	}

	public static void d(String tag, String msg)
	{
		if (logLevel >= LEVEL_DEBUG)
		{
			Log.d(TAG + tag, msg);
		}
	}

	public static void v(String tag, String msg)
	{
		if (logLevel >= LEVEL_VERBOSE)
		{
			Log.v(TAG + tag, msg);
		}
	}

	public static void e(String tag, String fmt, String... fmtArgs)
	{
		e(tag, String.format(fmt, fmtArgs));
	}

	public static void w(String tag, String fmt, String... fmtArgs)
	{
		w(tag, String.format(fmt, fmtArgs));
	}

	public static void i(String tag, String fmt, String... fmtArgs)
	{
		i(tag, String.format(fmt, fmtArgs));
	}

	public static void d(String tag, String fmt, String... fmtArgs)
	{
		d(tag, String.format(fmt, fmtArgs));
	}

	public static void v(String tag, String fmt, String... fmtArgs)
	{
		v(tag, String.format(fmt, fmtArgs));
	}
}
