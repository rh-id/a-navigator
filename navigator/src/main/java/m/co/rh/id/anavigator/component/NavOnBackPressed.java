package m.co.rh.id.anavigator.component;

import android.app.Activity;
import android.view.View;

/**
 * implement this on StatefulView if you need to handle onBackPressed event from activity.
 * Navigator will forward onBackPressed event to ONLY current active StatefulView (currently displayed view)
 */
public interface NavOnBackPressed<ACT extends Activity> {
    /**
     * when back button is pressed
     *
     * @param currentView current view of the StatefulView
     * @param activity    current activity
     * @param navigator   current instance of the navigator
     */
    void onBackPressed(View currentView, ACT activity, INavigator navigator);
}
