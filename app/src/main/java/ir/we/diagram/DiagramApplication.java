package ir.we.diagram;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import org.telegram.messenger.NativeLoader;
import org.telegram.tgnet.ConnectionsManager;

import java.io.File;

public class DiagramApplication extends Application {
    public static final int APP_ID = 1234;
    private static DiagramApplication sInstance;

    public static DiagramApplication getInstance() {
        return sInstance;
    }



    public static File getFilesDirFixed() {
        for (int a = 0; a < 10; a++) {
            File path = getInstance().getFilesDir();
            if (path != null) {
                return path;
            }
        }
        try {
            ApplicationInfo info = getInstance().getApplicationInfo();
            File path = new File(info.dataDir, "files");
            path.mkdirs();
            return path;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new File("/data/data/ir.we.diagram/files");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        NativeLoader.initNativeLibs(this);
        ConnectionsManager.native_setJava(Build.VERSION.SDK_INT == 14 || Build.VERSION.SDK_INT == 15);
    }

}