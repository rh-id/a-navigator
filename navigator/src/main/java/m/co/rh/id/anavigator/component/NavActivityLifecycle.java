package m.co.rh.id.anavigator.component;

import android.app.Activity;

/**
 * implement this on StatefulView if you need to handle activity lifecycle.
 * Navigator will forward activity lifecycle events to ALL initialized StatefulView in stack
 */
public interface NavActivityLifecycle<ACT extends Activity> {
    /**
     * Listen to Activity.onResume event.
     * this is called after StatefulView.initState and StatefulView.buildView
     */
    default void onResume(ACT activity) {
        // Default leave blank
    }

    /**
     * Listen to Activity.onPause event.
     */
    default void onPause(ACT activity) {
        // Default leave blank
    }
}
