package m.co.rh.id.anavigator.component;

import android.app.Activity;
import android.view.View;

import java.io.Serializable;

/**
 * Callback when navigator pops
 */
public interface NavPopCallback<ACT extends Activity, RESULT extends Serializable> extends Serializable {
    /**
     * Callback when pop navigator stack
     *
     * @param activity    current activity
     * @param currentView current view (NOT the pushed view)
     * @param result      result from the pushed view
     */
    void onPop(ACT activity, View currentView, RESULT result);
}
