package m.co.rh.id.anavigator;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import java.io.Serializable;

/**
 * A navigator that lives inside a ViewGroup of a host page (nested navigator),
 * it has its own route stack and its own {@link NavViewLayout}.
 * <p>
 * Predictive back: this class inherits the full predictive back gesture
 * support ({@link #shouldInterceptBack()}, {@link #onBackStarted(NavBackEvent)},
 * {@link #onBackProgressed(NavBackEvent)}, {@link #onBackCancelled()}) from
 * {@link Navigator}, and since the route stack and the view animator lookup
 * ({@link #getViewAnimator()}) are instance scoped, every inherited gesture
 * rule automatically applies to this navigator's own nested stack. Because
 * gesture animations only start when the stack size is more than 1, the commit
 * path can never reach the initial route, therefore the existing behavior of
 * never finishing any activity at the initial route is preserved (host pages
 * forward the gesture events to this navigator manually, so this is secondary
 * but consistent).
 */
@SuppressWarnings({"rawtypes"})
class ViewNavigator<ACT extends Activity, SV extends StatefulView> extends Navigator<ACT, SV> {
    private int mViewGroupContainerId;

    public ViewNavigator(Class<ACT> activityClass, NavConfiguration<ACT, SV> navConfiguration, int viewGroupContainerId) {
        super(activityClass, navConfiguration);
        mViewGroupContainerId = viewGroupContainerId;
    }

    @Override
    protected NavViewLayout getViewAnimator() {
        Activity activity = getActivity();
        if (activity == null) {
            return null;
        }
        ViewGroup viewGroup = activity.findViewById(mViewGroupContainerId);
        if (viewGroup == null) {
            return null;
        }
        return viewGroup.findViewById(getViewAnimatorId());
    }

    @Override
    protected void setViewAnimator(ACT activity, NavViewLayout viewAnimator) {
        ViewGroup viewGroup = activity.findViewById(mViewGroupContainerId);
        if (viewGroup != null) {
            viewGroup.addView(viewAnimator);
        }
    }

    @Override
    public boolean pop(Serializable result) {
        if (isInitialRoute()) {
            return false;
        } else {
            return super.pop(result);
        }
    }

    @Override
    protected void initViewAnimator() {
        ACT activity = getActivity();
        if (activity != null) {
            ViewGroup viewGroup = activity.findViewById(mViewGroupContainerId);
            if (viewGroup != null) {
                NavViewLayout viewAnimator = getViewAnimator();
                if (viewAnimator == null) {
                    super.initViewAnimator();
                }
            }
        }
    }

    void tryReset(View currentView) {
        if (currentView != null) {
            ViewGroup viewGroup = currentView.findViewById(mViewGroupContainerId);
            if (viewGroup != null && viewGroup.findViewById(getViewAnimatorId()) != null) {
                while (!isInitialRoute()) {
                    pop();
                }
                popInitialRoute(null);
                viewGroup.removeAllViews();
                removeAllOnRouteChangedListener();
            }
        }
    }

    int getViewGroupContainerId() {
        return mViewGroupContainerId;
    }
}