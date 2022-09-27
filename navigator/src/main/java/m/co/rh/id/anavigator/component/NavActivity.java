package m.co.rh.id.anavigator.component;

import android.content.pm.ActivityInfo;


public interface NavActivity {

    /**
     * Implement this on StatefulView if you want navigator to handle default orientation.
     * Navigator will handle Activity.setRequestedOrientation call when user navigate (push) to StatefulView that implements this interface.
     * Navigator will also rollback the requested orientation when user leave (pop) StatefulView route.
     * When navigator restore states it will also set the requested orientation automatically.
     */
    interface RequestOrientation {

        /**
         * Requested orientation values that you want Navigator to set.
         *
         * @return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED by default
         * @see <a href="https://developer.android.com/reference/android/app/Activity#setRequestedOrientation(int)">possible values</a>
         */
        default int getRequestedOrientation() {
            return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        }
    }
}
