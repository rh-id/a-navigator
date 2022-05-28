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
    default void onTrimMemory(int flag) {
        // Default leave blank
    }

    /**
     * Listen to ComponentCallbacks.onConfigurationChanged.
     */
    default void onConfigurationChanged(Configuration configuration) {
        // Default leave blank
    }

    /**
     * Listen to ComponentCallbacks.onConfigurationChanged.
     */
    default void onLowMemory() {
        // Default leave blank
    }
}
