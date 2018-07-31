package cz.destil.moodsync.core;

import android.app.Application;

import com.crittercism.app.Crittercism;
import com.philips.lighting.hue.sdk.wrapper.HueLog;
import com.philips.lighting.hue.sdk.wrapper.Persistence;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import cz.destil.moodsync.BuildConfig;
import cz.destil.moodsync.light.LightsController;
import cz.destil.moodsync.light.MirroringHelper;

/**
 * Main application object.
 *
 * @author David Vávra (david@vavra.me)
 */
public class App extends Application {

    static App sInstance;
    static Bus sBus;
    private MirroringHelper mMirroring;
    private LightsController mLights;

    static {
        // Load the huesdk native library before calling any SDK method
        System.loadLibrary("huesdk");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!BuildConfig.DEBUG) {
            Crittercism.initialize(this, "552c06ab7365f84f7d3d6da5");
        }

        // Configure the storage location and log level for the Hue SDK
        Persistence.setStorageLocation(getFilesDir().getAbsolutePath(), "HueQuickStart");
        HueLog.setConsoleLogLevel(HueLog.LogLevel.INFO);

        sInstance = this;
        sBus = new Bus(ThreadEnforcer.ANY);
        mMirroring = MirroringHelper.get();
        mLights = LightsController.get();
        mMirroring.init();
        mLights.init();
    }

    public static App get() {
        return sInstance;
    }

    public static Bus bus() {
        return sBus;
    }
}
