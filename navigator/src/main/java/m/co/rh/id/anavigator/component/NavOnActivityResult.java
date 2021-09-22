package m.co.rh.id.anavigator.component;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

/**
 * implement this on StatefulView if you need to handle onActivityResult event from activity.
 * Navigator will forward onActivityResult event to ONLY current active StatefulView (currently displayed view)
 */
public interface NavOnActivityResult<ACT extends Activity> {
    /**
     * when onActivityResult return value
     *
     * @param currentView current view of the StatefulView
     * @param activity    current activity
     * @param INavigator  current instance of the navigator
     * @param requestCode request code forwarded from onActivityResult
     * @param resultCode  result code forwarded from onActivityResult
     * @param data        intent data forwarded from onActivityResult
     */
    void onActivityResult(View currentView, ACT activity, INavigator INavigator,
                          int requestCode, int resultCode, Intent data);
}
