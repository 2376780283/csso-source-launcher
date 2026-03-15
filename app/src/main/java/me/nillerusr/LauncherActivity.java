package me.nillerusr;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.libsdl.app.SDLActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import me.dmk95.csso.R;
import me.sanyasho.util.SharedUtil;

public class LauncherActivity extends Activity
{
	public static String TAG = "LauncherActivity";

	public static String MOD_NAME = "csso";
	public static String PKG_NAME;

	static EditText cmdArgs = null, GamePath = null;
	public SharedPreferences mPref;

	final static int REQUEST_PERMISSIONS = 42;

	public void applyPermissions( final String[] permissions, final int code )
	{
		List< String > requestPermissions = new ArrayList<>();
		for( String permission : permissions )
		{
			if( checkSelfPermission( permission ) != PackageManager.PERMISSION_GRANTED )
				requestPermissions.add( permission );
		}

		if( !requestPermissions.isEmpty() )
		{
			String[] requestPermissionsArray = new String[ requestPermissions.size() ];
			for( int i = 0; i < requestPermissions.size(); i++ )
				requestPermissionsArray[ i ] = requestPermissions.get( i );
			requestPermissions( requestPermissionsArray, code );
		}
	}

	public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults )
	{
		if( requestCode == REQUEST_PERMISSIONS )
		{
			boolean allGranted = true;

			// For Android 11+, check MANAGE_EXTERNAL_STORAGE permission
			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.R )
			{
				if( !Environment.isExternalStorageManager() )
				{
					allGranted = false;
				}
			}

			// Check other permissions (RECORD_AUDIO)
			if( grantResults.length > 0 )
			{
				for( int result : grantResults )
				{
					if( result == PackageManager.PERMISSION_DENIED )
					{
						allGranted = false;
						break;
					}
				}
			}

			if( !allGranted )
			{
				Toast.makeText( this, R.string.srceng_launcher_error_no_permission, Toast.LENGTH_LONG ).show();
				finish();
			}
		}
	}

	public static String getDefaultDir()
	{
		File dir = Environment.getExternalStorageDirectory();
		if( dir == null || !dir.exists() )
			return "/sdcard/";
		return dir.getPath();
	}

	public void onCreate( Bundle savedInstanceState )
	{
		// Initialize crash handler
		CrashHandler.init(this);

		super.onCreate( savedInstanceState );
		PKG_NAME = getApplication().getPackageName();
		requestWindowFeature( Window.FEATURE_NO_TITLE );

		super.setTheme( 0x01030224 );

		mPref = getSharedPreferences( "mod", 0 );

		setContentView( R.layout.activity_launcher );

		// Enable fullscreen mode and ignore cutout (notch) area
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.P )
		{
			// Android 9 (API 28) and above - use display cutout mode
			getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
		}

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.R )
		{
			// Android 11 (API 30) and above - use WindowInsetsController
			getWindow().setDecorFitsSystemWindows( false );
			getWindow().getInsetsController().hide( android.view.WindowInsets.Type.systemBars() );
			getWindow().getInsetsController().setSystemBarsBehavior( android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE );
		}
		else
		{
			// Android 10 and below - use legacy fullscreen flags
			getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_FULLSCREEN |
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			);
		}

		cmdArgs = findViewById( R.id.edit_cmdline );
		GamePath = findViewById( R.id.edit_gamepath );

		Button button = findViewById( R.id.button_launch );
		button.setOnClickListener( LauncherActivity.this::startSource );

		Button aboutButton = findViewById( R.id.button_about );
		aboutButton.setOnClickListener( this::aboutEngine );

		Button dirButton = findViewById( R.id.button_gamedir );
		dirButton.setOnClickListener( v ->
		{
			Intent intent = new Intent( LauncherActivity.this, DirchActivity.class );
			intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
			startActivity( intent );
		} );

		cmdArgs.setText( mPref.getString( "argv", getString( R.string.default_commandline_arguments ) ) );
		GamePath.setText( mPref.getString( "gamepath", getDefaultDir() + "/srceng" ) );

		// Display total play time
		TextView playTimeText = findViewById( R.id.text_play_time );
		long totalPlayTimeMs = SDLActivity.getTotalPlayTime( this );
		long totalHours = totalPlayTimeMs / (1000 * 60 * 60);
		long totalMinutes = (totalPlayTimeMs % (1000 * 60 * 60)) / (1000 * 60);
		playTimeText.setText( totalHours + "h " + totalMinutes + "m" );

		// View crash log button
		Button crashLogButton = findViewById( R.id.button_view_crash_log );
		crashLogButton.setOnClickListener( v ->
		{
			Intent intent = new Intent( LauncherActivity.this, CrashActivity.class );
			intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
			startActivity( intent );
		} );

		// permissions check based on Android version
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.R )
		{
			// Android 11 (API 30) and above - check MANAGE_EXTERNAL_STORAGE
			if( !Environment.isExternalStorageManager() )
			{
				// Show dialog to guide user to settings
				new AlertDialog.Builder( this )
					.setTitle( "权限要求" )
					.setMessage( "需要访问所有文件权限来运行游戏。请在设置中开启此权限。" )
					.setPositiveButton( "去设置", ( dialog, which ) -> {
						Intent intent = new Intent( android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION );
						startActivity( intent );
					} )
					.setNegativeButton( "取消", ( dialog, which ) -> {
						Toast.makeText( this, R.string.srceng_launcher_error_no_permission, Toast.LENGTH_LONG ).show();
						finish();
					} )
					.setCancelable( false )
					.show();
			}
			// RECORD_AUDIO can be requested normally
			applyPermissions( new String[] { Manifest.permission.RECORD_AUDIO }, REQUEST_PERMISSIONS );
		}
		else
		{
			// Android 10 (API 29) and below - use traditional storage permission
			applyPermissions( new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO }, REQUEST_PERMISSIONS );
		}

		boolean isCommitEmpty = getResources().getString( R.string.current_commit ).isEmpty();
		boolean isURLEmpty = getResources().getString( R.string.update_url ).isEmpty();

		if( !isCommitEmpty && !isURLEmpty )
		{
			UpdateSystem upd = new UpdateSystem( this );
			upd.execute();
		}
	}

	public void aboutEngine( View view )
	{
		final Activity a = this;
		this.runOnUiThread( () ->
		{
			final Dialog dialog = new Dialog( a );
			dialog.requestWindowFeature( Window.FEATURE_NO_TITLE ); // hide the dialog title
			dialog.setContentView( R.layout.about );
			dialog.setCancelable( true );
			dialog.show();

			TextView Links = dialog.findViewById( R.id.about_links );
			Links.setMovementMethod( LinkMovementMethod.getInstance() );

			dialog.findViewById( R.id.about_button_ok ).setOnClickListener( v -> dialog.cancel() );

		} );
	}

	public void saveSettings( SharedPreferences.Editor editor )
	{
		String argv = SharedUtil.prepareArgv( cmdArgs.getText().toString() );
		String gamepath = GamePath.getText().toString();

		editor.putString( "argv", argv );
		editor.putString( "gamepath", gamepath );
		editor.commit();
	}

	private Intent prepareIntent( Intent i )
	{
		String argv = SharedUtil.prepareArgv( cmdArgs.getText().toString() );
		i.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
		saveSettings( mPref.edit() );

		if( argv.length() != 0 )
			i.putExtra( "argv", argv );

		i.putExtra( "gamedir", MOD_NAME );

		return i;
	}

	public void startSource( View view )
	{
		String argv = SharedUtil.prepareArgv( cmdArgs.getText().toString() );
		SharedPreferences.Editor editor = mPref.edit();
		editor.putString( "argv", argv );

		if( argv.contains( "-game " ) )
		{
			new AlertDialog.Builder( this )
					.setTitle( R.string.srceng_launcher_error )
					.setMessage( R.string.csso_game_check )
					.setPositiveButton( R.string.srceng_launcher_ok, null )
					.show();

			return;
		}

		saveSettings( editor );

		Intent intent = new Intent( LauncherActivity.this, SDLActivity.class );
		intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
		intent = prepareIntent( intent );
		startActivity( intent );
	}

	public void onPause()
	{
		saveSettings( mPref.edit() );
		super.onPause();
	}
}
