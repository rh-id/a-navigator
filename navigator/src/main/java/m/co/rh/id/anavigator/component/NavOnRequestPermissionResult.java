package m.co.rh.id.anavigator.component;

import android.app.Activity;
import android.view.View;

/**
 * implement this on StatefulView if you need to handle onRequestPermissionsResult event from activity.
 * Navigator will forward onRequestPermissionsResult event to ONLY current active StatefulView (currently displayed view)
 */
public interface NavOnRequestPermissionResult<ACT extends Activity> {
    /**
     * when onActivityResult return value
     *
     * @param currentView  current view of the StatefulView, null if StatefulViewDialog
     * @param activity     current activity
     * @param INavigator   current instance of the navigator
     * @param requestCode  request code forwarded from onRequestPermissionsResult
     * @param permissions  permissions forwarded from onRequestPermissionsResult
     * @param grantResults permissions forwarded from onRequestPermissionsResult
     */
    void onRequestPermissionsResult(View currentView, ACT activity, INavigator INavigator,
                                    int requestCode, String[] permissions, int[] grantResults);
}
