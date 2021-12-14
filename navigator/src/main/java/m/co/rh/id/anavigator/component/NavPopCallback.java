package m.co.rh.id.anavigator.component;

import android.app.Activity;
import android.view.View;

import java.io.Serializable;

import m.co.rh.id.anavigator.NavRoute;

/**
 * Callback when navigator pops
 */
public interface NavPopCallback<ACT extends Activity> extends Serializable {
    /**
     * Callback when pop navigator stack
     *
     * @param navigator   current navigator
     * @param navRoute    current navRoute that gets pop
     * @param activity    current activity
     * @param currentView current view (NOT the pushed view)
     */
    void onPop(INavigator navigator, NavRoute navRoute, ACT activity, View currentView);
}
