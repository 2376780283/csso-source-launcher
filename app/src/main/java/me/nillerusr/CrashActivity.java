package me.nillerusr;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import me.dmk95.csso.R;

public class CrashActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_crash);

		TextView crashLogText = findViewById(R.id.crash_log_text);
		String crashLog = CrashHandler.getCrashLog(this);
		crashLogText.setText(crashLog);

		Button copyButton = findViewById(R.id.button_copy_log);
		copyButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				copyLogToClipboard();
			}
		});

		Button clearButton = findViewById(R.id.button_clear_log);
		clearButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				clearCrashLog();
			}
		});

		Button closeButton = findViewById(R.id.button_close);
		closeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
			}
		});
	}

	private void copyLogToClipboard()
	{
		try
		{
			String crashLog = CrashHandler.getCrashLog(this);
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			android.content.ClipData clip = android.content.ClipData.newPlainText("Crash Log", crashLog);
			clipboard.setPrimaryClip(clip);
			Toast.makeText(this, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show();
		}
		catch (Exception e)
		{
			Toast.makeText(this, "复制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	private void clearCrashLog()
	{
		CrashHandler.clearCrashLog(this);
		TextView crashLogText = findViewById(R.id.crash_log_text);
		crashLogText.setText("日志已清除");
		Toast.makeText(this, "崩溃日志已清除", Toast.LENGTH_SHORT).show();
	}
}
