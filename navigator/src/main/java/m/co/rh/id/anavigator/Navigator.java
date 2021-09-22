package m.co.rh.id.anavigator;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import java.io.Serializable;
import java.util.LinkedList;

import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.NavActivityLifecycle;
import m.co.rh.id.anavigator.component.NavComponentCallback;
import m.co.rh.id.anavigator.component.NavOnActivityResult;
import m.co.rh.id.anavigator.component.NavOnBackPressed;
import m.co.rh.id.anavigator.component.NavPopCallback;
import m.co.rh.id.anavigator.component.RequireNavigator;
import m.co.rh.id.anavigator.component.StatefulViewFactory;
import m.co.rh.id.anavigator.exception.NavigationRouteNotFound;

/**
 * Navigator to handle navigation in an activity. Register this on Application level
 *
 * @param <ACT> Activity class type
 * @param <SV>  StatefulViewHandler type
 */
@SuppressWarnings("unchecked")
public class Navigator<ACT extends Activity, SV extends StatefulView> implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2, INavigator {
    /**
     * Key used to retrieve value from Intent data when navigator {@link #pop(Object)} and finishes the activity.
     * If an activity called startActivityForResult and waiting for result at onActivityResult,
     * use this to retrieve the data (intent.getSerializableExtra(ACTIVITY_RESULT_SERIALIZABLE_KEY))
     */
    public static final String ACTIVITY_RESULT_SERIALIZABLE_KEY = "onActivityResult.serializableResult";

    private Class<ACT> mActivityClass;
    private ACT mActivity;
    private NavConfiguration<ACT, SV> mNavConfiguration;
    private LinkedList<NavRoute> mNavRouteStack;
    private int mContainerId;
    private boolean mIsNavigating;
    private LinkedList<Runnable> mPendingNavigatorRoute;

    /**
     * @param activityClass activity class that this navigator handles
     */
    public Navigator(Class<ACT> activityClass, NavConfiguration<ACT, SV> navConfiguration) {
        if (navConfiguration == null) {
            throw new IllegalStateException("navConfiguration must not null");
        }
        mActivityClass = activityClass;
        mNavConfiguration = navConfiguration;
        mNavRouteStack = new LinkedList<>();
        mContainerId = Integer.MAX_VALUE;
        mPendingNavigatorRoute = new LinkedList<>();
    }

    @Override
    public <POPRESULT extends Object> void push(String routeName, Object args, NavPopCallback<POPRESULT> navPopCallback) {
        if (mIsNavigating) {
            // It is possible that push is invoked somewhere during initState or buildView
            // put this invocation later after previous push is done
            mPendingNavigatorRoute.add(() -> push(routeName, args, navPopCallback));
            return;
        }
        mIsNavigating = true;
        StatefulViewFactory<ACT, SV> statefulViewFactory = mNavConfiguration.getNavMap().get(routeName);
        if (statefulViewFactory == null) {
            mIsNavigating = false;
            throw new NavigationRouteNotFound(routeName + " not found");
        }
        SV statefulView = statefulViewFactory.newInstance(args, mActivity);
        // push must be done before initState or buildView so that the statefulView could get route information
        mNavRouteStack.push(new NavRoute(navPopCallback, statefulView, routeName, args, statefulView.getKey()));
        if (statefulView instanceof RequireNavigator) {
            ((RequireNavigator) statefulView).provideNavigator(this);
        }
        View view = statefulView.buildView(mActivity);
        ViewAnimator existingViewAnimator = mActivity.findViewById(mContainerId);
        existingViewAnimator.addView(view);
        existingViewAnimator.showNext();
        mIsNavigating = false;
        if (!mPendingNavigatorRoute.isEmpty()) {
            mPendingNavigatorRoute.pop().run();
        }
    }

    @Override
    public void push(String routeName, Serializable args) {
        push(routeName, args, null);
    }

    @Override
    public void push(String routeName) {
        push(routeName, null, null);
    }

    @Override
    public boolean pop(Object result) {
        if (mIsNavigating) {
            // if this pop is invoke during initState or buildView or dispose, add to pending
            mPendingNavigatorRoute.add(() -> pop(result));
            return false;
        }
        mIsNavigating = true;
        if (mNavRouteStack.size() > 1) {
            ViewAnimator existingViewAnimator = mActivity.findViewById(mContainerId);
            View currentView = existingViewAnimator.getCurrentView();
            existingViewAnimator.showPrevious();
            popStack(result);
            existingViewAnimator.removeView(currentView);
            mIsNavigating = false;
            if (!mPendingNavigatorRoute.isEmpty()) {
                mPendingNavigatorRoute.pop().run();
            }
            return true;
        } else {
            // pop initial route
            if (mNavRouteStack.size() == 1) {
                popStack(null);
            }
            int activityResult = Activity.RESULT_CANCELED;
            if (result != null) {
                activityResult = Activity.RESULT_OK;
            }
            setActivityResultAndFinish(activityResult, result);
        }
        mIsNavigating = false;
        if (!mPendingNavigatorRoute.isEmpty()) {
            mPendingNavigatorRoute.pop().run();
        }
        return false;
    }

    @Override
    public void finishActivity(Object result) {
        // dispose every stack and clear it
        while (!mNavRouteStack.isEmpty()) {
            StatefulView statefulView = mNavRouteStack.pop().getStatefulView();
            statefulView.dispose(mActivity);
        }
        mPendingNavigatorRoute.clear();
        mIsNavigating = false;
        setActivityResultAndFinish(Activity.RESULT_OK, result);
    }

    private void setActivityResultAndFinish(Integer activityResult, Object result) {
        Intent data = null;
        if (result != null) {
            if (result instanceof Intent) {
                data = (Intent) result;
            } else if (result instanceof Serializable) {
                data = new Intent();
                data.putExtra(ACTIVITY_RESULT_SERIALIZABLE_KEY, (Serializable) result);
            }
        }
        mActivity.setResult(activityResult, data);
        mActivity.finish();
    }

    @Override
    public boolean pop() {
        return pop(null);
    }

    @Override
    public NavConfiguration getNavConfiguration() {
        return mNavConfiguration;
    }

    @Override
    public NavRoute getCurrentRoute() {
        return mNavRouteStack.peek();
    }

    @Override
    public void onBackPressed() {
        if (!mNavRouteStack.isEmpty()) {
            StatefulView statefulView = mNavRouteStack.peek().getStatefulView();
            if (statefulView instanceof NavOnBackPressed) {
                ViewAnimator viewAnimator = mActivity.findViewById(mContainerId);
                ((NavOnBackPressed) statefulView).onBackPressed(viewAnimator.getCurrentView(),
                        mActivity, this);
                return;
            }
        }
        // if back button is not handled, then pop as usual
        pop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mNavRouteStack.isEmpty()) {
            StatefulView statefulView = mNavRouteStack.peek().getStatefulView();
            if (statefulView instanceof NavOnActivityResult) {
                ViewAnimator viewAnimator = mActivity.findViewById(mContainerId);
                ((NavOnActivityResult) statefulView).onActivityResult(viewAnimator.getCurrentView(),
                        mActivity, this, requestCode, resultCode, data);
                return;
            }
        }
    }

    @Override
    public boolean isInitialRoute() {
        return mNavRouteStack.size() == 1 ? true : false;
    }

    private void popStack(Object result) {
        NavRoute currentNavRoute = mNavRouteStack.pop();
        currentNavRoute.setRouteResult(result);
        StatefulView statefulView = currentNavRoute.getStatefulView();
        if (statefulView != null) {
            statefulView.dispose(mActivity);
        }
        NavPopCallback navPopCallback = currentNavRoute.getNavPopCallback();
        if (navPopCallback != null) {
            navPopCallback.onPop(result);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        if (mActivityClass.isInstance(activity)) {
            mActivity = (ACT) activity;
            ViewAnimator viewAnimator = new ViewAnimator(mActivity);
            viewAnimator.setId(mContainerId);
            viewAnimator.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            viewAnimator.setInAnimation(mNavConfiguration.getDefaultInAnimation());
            viewAnimator.setOutAnimation(mNavConfiguration.getDefaultOutAnimation());
            viewAnimator.setAnimateFirstView(true);
            mActivity.setContentView(viewAnimator);
            if (!mNavRouteStack.isEmpty()) {
                // re-add all the views
                for (NavRoute navRoute : mNavRouteStack) {
                    viewAnimator.addView(navRoute
                            .getStatefulView()
                            .buildView(mActivity));
                }
            } else {
                push(mNavConfiguration.getInitialRouteName());
            }
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // leave blank
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (mActivityClass.isInstance(activity)) {
            if (!mNavRouteStack.isEmpty()) {
                for (NavRoute navRoute : mNavRouteStack) {
                    StatefulView statefulView = navRoute.getStatefulView();
                    if (statefulView instanceof NavActivityLifecycle) {
                        ((NavActivityLifecycle) statefulView).onResume(mActivity);
                    }
                }
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (mActivityClass.isInstance(activity)) {
            if (!mNavRouteStack.isEmpty()) {
                for (NavRoute navRoute : mNavRouteStack) {
                    StatefulView statefulView = navRoute.getStatefulView();
                    if (statefulView instanceof NavActivityLifecycle) {
                        ((NavActivityLifecycle) statefulView).onPause(mActivity);
                    }
                }
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        // leave empty
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        // leave empty
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (mActivityClass.isInstance(activity)) {
            mActivity = null;
        }
    }

    @Override
    public void onTrimMemory(int flag) {
        if (!mNavRouteStack.isEmpty()) {
            for (NavRoute navRoute : mNavRouteStack) {
                StatefulView statefulView = navRoute.getStatefulView();
                if (statefulView instanceof NavComponentCallback) {
                    ((NavComponentCallback) statefulView).onTrimMemory(flag);
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        if (!mNavRouteStack.isEmpty()) {
            for (NavRoute navRoute : mNavRouteStack) {
                StatefulView statefulView = navRoute.getStatefulView();
                if (statefulView instanceof NavComponentCallback) {
                    ((NavComponentCallback) statefulView).onConfigurationChanged(configuration);
                }
            }
        }
    }

    @Override
    public void onLowMemory() {
        if (!mNavRouteStack.isEmpty()) {
            for (NavRoute navRoute : mNavRouteStack) {
                StatefulView statefulView = navRoute.getStatefulView();
                if (statefulView instanceof NavComponentCallback) {
                    ((NavComponentCallback) statefulView).onLowMemory();
                }
            }
        }
    }
}
