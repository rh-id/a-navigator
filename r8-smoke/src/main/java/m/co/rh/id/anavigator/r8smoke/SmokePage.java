package m.co.rh.id.anavigator.r8smoke;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import m.co.rh.id.anavigator.NavRoute;
import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.annotation.NavInject;
import m.co.rh.id.anavigator.annotation.NavRouteIndex;
import m.co.rh.id.anavigator.annotation.NavViewNavigator;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.NavOnRouteChangedListener;

/**
 * The page that exercises every reflection-injected field kind supported by
 * Navigator.injectStatefulView. If the annotations are stripped by R8 (missing
 * consumer rules), the fields stay null/0 and SmokeResult records the failure -
 * the test APK never touches these fields directly, so nothing in the test
 * build keeps them.
 */
public class SmokePage extends StatefulView<Activity> {

    @NavInject
    private transient INavigator mNavigator;

    @NavInject
    private transient ISmokeComponent mComponent;

    @NavRouteIndex
    private transient Integer mRouteIndex;

    @NavRouteIndex
    private transient int mPrimitiveIndex;

    @NavViewNavigator("smoke_container")
    private transient INavigator mViewNavigator;

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        // Injection happened before buildView, so all annotation-injected fields
        // are final here - except the child route inside the ViewNavigator, which
        // is pushed right after this page's view is attached (see the listener below).
        SmokeResult.navigatorInjected = mNavigator != null
                && mNavigator.getActivity() != null;
        SmokeResult.componentInjected = mComponent != null
                && SmokeComponent.EXPECTED_VALUE.equals(mComponent.getValue());
        SmokeResult.routeIndexWrapperOk = mRouteIndex != null
                && mRouteIndex == SmokeResult.EXPECTED_ROUTE_INDEX;
        SmokeResult.routeIndexPrimitiveOk = mRouteIndex != null
                && mPrimitiveIndex == SmokeResult.EXPECTED_ROUTE_INDEX;

        View view = activity.getLayoutInflater().inflate(R.layout.page_smoke, container, false);
        if (mViewNavigator == null) {
            // @NavViewNavigator injection failed: final failure state, no need to wait.
            SmokeResult.viewNavigatorInjected = false;
            SmokeResult.childNavigatorInjected = false;
            SmokeResult.done();
        } else {
            // The ViewNavigator renders its initial route (SmokeChildPage) into
            // R.id.smoke_container shortly after this page is attached; when that
            // route change fires, every injection outcome is final.
            mViewNavigator.addOnRouteChangedListener(mRouteChangedListener);
        }
        return view;
    }

    private final transient NavOnRouteChangedListener mRouteChangedListener =
            new NavOnRouteChangedListener() {
                @Override
                public void onChanged(NavRoute previous, NavRoute current) {
                    // Reached only when mViewNavigator is a live, container-attached
                    // ViewNavigator that actually pushed its child route.
                    SmokeResult.viewNavigatorInjected = true;
                    SmokeResult.childNavigatorInjected = current != null;
                    SmokeResult.done();
                }
            };

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        if (mViewNavigator != null) {
            mViewNavigator.removeOnRouteChangedListener(mRouteChangedListener);
        }
        mViewNavigator = null;
        mNavigator = null;
        mComponent = null;
    }
}
