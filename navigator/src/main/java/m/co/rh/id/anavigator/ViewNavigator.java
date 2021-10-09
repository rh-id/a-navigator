package m.co.rh.id.anavigator;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import java.io.Serializable;

@SuppressWarnings({"rawtypes"})
class ViewNavigator<ACT extends Activity, SV extends StatefulView> extends Navigator<ACT, SV> {
    private int mViewGroupContainerId;

    public ViewNavigator(Class<ACT> activityClass, NavConfiguration<ACT, SV> navConfiguration, int viewGroupContainerId) {
        super(activityClass, navConfiguration);
        mViewGroupContainerId = viewGroupContainerId;
    }

    @Override
    protected ViewAnimator getViewAnimator() {
        ViewGroup viewGroup = getActivity().findViewById(mViewGroupContainerId);
        if (viewGroup == null) {
            return null;
        }
        return viewGroup.findViewById(getViewAnimatorId());
    }

    @Override
    protected void setViewAnimator(ACT activity, ViewAnimator viewAnimator) {
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
                ViewAnimator viewAnimator = getViewAnimator();
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