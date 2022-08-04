package m.co.rh.id.anavigator.component;

import android.app.Activity;

/**
 * implement this on StatefulView if you need to handle activity lifecycle.
 * Navigator will forward activity lifecycle events to ALL initialized StatefulView in stack
 */
public interface NavActivityLifecycle<ACT extends Activity> {
    /**
     * Listen to Activity.onStart event.
     */
    default void onNavActivityStarted(ACT activity) {
        // Default leave blank
    }

    /**
     * Listen to Activity.onResume event.
     */
    default void onNavActivityResumed(ACT activity) {
        // Default leave blank
    }

    /**
     * Listen to Activity.onPause event.
     */
    default void onNavActivityPaused(ACT activity) {
        // Default leave blank
    }

    /**
     * Listen to Activity.onStop event.
     */
    default void onNavActivityStopped(ACT activity) {
        // Default leave blank
    }
}
