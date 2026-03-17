package me.nillerusr;

import android.app.Application;
import com.kyant.fishnet.Fishnet;
import java.io.File;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        File logsDir = new File(getFilesDir(), "logs");
        logsDir.mkdirs();
        String logPath = logsDir.getAbsolutePath();
        Fishnet.init(this, logPath);
    }
}
