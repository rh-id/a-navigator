package m.co.rh.id.anavigator.component;

import android.content.res.Configuration;

/**
 * implement this on StatefulView if you need to handle ComponentCallbacks2.
 * Navigator will forward ComponentCallbacks2 events to ALL initialized StatefulView in stack
 */
public interface NavComponentCallback {
    /**
     * Listen to ComponentCallbacks2.onTrimMemory.
     */
    void onTrimMemory(int flag);

    /**
     * Listen to ComponentCallbacks.onConfigurationChanged.
     */
    void onConfigurationChanged(Configuration configuration);

    /**
     * Listen to ComponentCallbacks.onConfigurationChanged.
     */
    void onLowMemory();
}
