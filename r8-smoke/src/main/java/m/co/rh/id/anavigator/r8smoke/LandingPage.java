package m.co.rh.id.anavigator.r8smoke;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.RequireNavigator;

/**
 * Initial route "/". Forwards to SmokePage right after the initial route push
 * completes, so SmokePage ends up at route index 1 (never 0, which is the
 * default value of an int field and would make the @NavRouteIndex checks pass
 * even with a broken injection). This is the framework-blessed auto-forward
 * pattern (see the example module's SplashPage): a push issued from
 * MainActivity.onCreate would crash because the navigator only learns about
 * the activity in its ActivityLifecycleCallbacks.onActivityCreated.
 */
public class LandingPage extends StatefulView<Activity> implements RequireNavigator {

    private transient INavigator mNavigator;
    private transient boolean mForwarded;

    @Override
    public void provideNavigator(INavigator navigator) {
        mNavigator = navigator;
        if (!mForwarded) {
            mForwarded = true;
            mNavigator.push(SmokeApplication.ROUTE_SMOKE);
        }
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        FrameLayout frameLayout = new FrameLayout(activity);
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return frameLayout;
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        mNavigator = null;
    }
}
