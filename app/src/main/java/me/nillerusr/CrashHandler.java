package me.nillerusr;

import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler
{
	private static final String TAG = "CrashHandler";
	private static final String CRASH_LOG_FILE = "crash_log.txt";
	private static final String CRASH_DIR = "crashes";

	private Context mContext;
	private Thread.UncaughtExceptionHandler mDefaultHandler;

	public CrashHandler(Context context)
	{
		mContext = context.getApplicationContext();
		mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
	}

	public static void init(Context context)
	{
		Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
	}

	@Override
	public void uncaughtException(Thread thread, Throwable throwable)
	{
		try
		{
			// Save crash log to file
			saveCrashLog(throwable);

			// Show crash activity
			Intent intent = new Intent(mContext, CrashActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			mContext.startActivity(intent);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Error handling crash: " + e.getMessage(), e);
		}
		finally
		{
			// Kill the process
			Process.killProcess(Process.myPid());
			System.exit(1);
		}
	}

	private void saveCrashLog(Throwable throwable)
	{
		try
		{
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);

			pw.println("================ Crash Log ================");
			pw.println("Time: " + timeStamp);
			pw.println("App Version: " + getAppVersion());
			pw.println("Android Version: " + android.os.Build.VERSION.RELEASE);
			pw.println("Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
			pw.println("===========================================");
			pw.println();

			throwable.printStackTrace(pw);
			pw.println();
			pw.println("===========================================");

			String crashLog = sw.toString();
			pw.close();

			// Save to file
			File crashDir = new File(mContext.getFilesDir(), CRASH_DIR);
			if (!crashDir.exists())
			{
				crashDir.mkdirs();
			}

			File crashFile = new File(crashDir, CRASH_LOG_FILE);
			FileOutputStream fos = new FileOutputStream(crashFile);
			fos.write(crashLog.getBytes());
			fos.close();

			Log.e(TAG, "Crash log saved: " + crashFile.getAbsolutePath());
		}
		catch (Exception e)
		{
			Log.e(TAG, "Error saving crash log: " + e.getMessage(), e);
		}
	}

	public static String getCrashLog(Context context)
	{
		try
		{
			File crashDir = new File(context.getFilesDir(), CRASH_DIR);
			File crashFile = new File(crashDir, CRASH_LOG_FILE);
			if (crashFile.exists())
			{
				java.util.Scanner scanner = new java.util.Scanner(crashFile);
				StringBuilder content = new StringBuilder();
				while (scanner.hasNextLine())
				{
					content.append(scanner.nextLine()).append("\n");
				}
				scanner.close();
				return content.toString();
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Error reading crash log: " + e.getMessage(), e);
		}
		return "No crash log found.";
	}

	public static boolean hasCrashLog(Context context)
	{
		try
		{
			File crashDir = new File(context.getFilesDir(), CRASH_DIR);
			File crashFile = new File(crashDir, CRASH_LOG_FILE);
			return crashFile.exists();
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public static void clearCrashLog(Context context)
	{
		try
		{
			File crashDir = new File(context.getFilesDir(), CRASH_DIR);
			File crashFile = new File(crashDir, CRASH_LOG_FILE);
			if (crashFile.exists())
			{
				crashFile.delete();
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Error clearing crash log: " + e.getMessage(), e);
		}
	}

	private String getAppVersion()
	{
		try
		{
			return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
		}
		catch (Exception e)
		{
			return "Unknown";
		}
	}
}
