package m.co.rh.id.anavigator.example;

import android.os.Bundle;

import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import m.co.rh.id.anavigator.NavRoute;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.NavBackEvent;
import m.co.rh.id.anavigator.component.NavOnRouteChangedListener;

/**
 * Predictive back wiring for the framework (full write-up in the README,
 * section "Predictive back gesture support"). The three non-obvious facts:
 * <ul>
 * <li>ONE {@link OnBackPressedCallback}, registered once for the whole
 * activity lifetime (the dispatcher removes it on destroy);</li>
 * <li>its enabled state is synced from a {@link NavOnRouteChangedListener} -
 * the single sync point, which also fires after a process restore
 * (previous == null) since the navigator announces restores;</li>
 * <li>the gesture COMMIT rides the legacy
 * {@code navigator.onBackPressed()} - no separate commit path.</li>
 * </ul>
 * Requires the manifest opt-in
 * {@code android:enableOnBackInvokedCallback="true"} (API 33+).
 */
public class AppCompatExampleActivity extends AppCompatActivity {

    private INavigator mNavigator;
    private NavOnRouteChangedListener mRouteChangedListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNavigator = MyApplication.of(this).getNavigator(this);
        // fail-safe for copy-pasters whose Application does not register a
        // navigator for this activity class: skip the whole wiring instead of
        // NPE-ing from the dispatcher later
        if (mNavigator == null) {
            return;
        }
        final OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // gesture commit (or legacy button press): pops the route or
                // forwards to the current page's NavOnBackPressed
                mNavigator.onBackPressed();
            }

            @Override
            public void handleOnBackStarted(@NonNull BackEventCompat backEvent) {
                mNavigator.onBackStarted(toNavBackEvent(backEvent));
            }

            @Override
            public void handleOnBackProgressed(@NonNull BackEventCompat backEvent) {
                mNavigator.onBackProgressed(toNavBackEvent(backEvent));
            }

            @Override
            public void handleOnBackCancelled() {
                mNavigator.onBackCancelled();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
        // the single enabled-state sync point: fires on every push/pop AND
        // once after a saved-state restore (previous == null)
        mRouteChangedListener = (NavRoute previous, NavRoute current) ->
                callback.setEnabled(mNavigator.shouldInterceptBack());
        mNavigator.addOnRouteChangedListener(mRouteChangedListener);
        callback.setEnabled(mNavigator.shouldInterceptBack());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // load-bearing leak guard: the navigator is registered on Application
        // level and outlives this activity, the listener must not retain it
        // (guarded consistently with the onCreate early-return above)
        if (mRouteChangedListener != null) {
            mNavigator.removeOnRouteChangedListener(mRouteChangedListener);
            mRouteChangedListener = null;
        }
    }

    private static NavBackEvent toNavBackEvent(BackEventCompat backEvent) {
        return new NavBackEvent(
                backEvent.getTouchX(),
                backEvent.getTouchY(),
                backEvent.getProgress());
    }
}
