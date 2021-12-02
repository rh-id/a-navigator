package m.co.rh.id.anavigator;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ViewAnimator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;

import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.NavActivityLifecycle;
import m.co.rh.id.anavigator.component.NavComponentCallback;
import m.co.rh.id.anavigator.component.NavOnActivityResult;
import m.co.rh.id.anavigator.component.NavOnBackPressed;
import m.co.rh.id.anavigator.component.NavOnRouteChangedListener;
import m.co.rh.id.anavigator.component.NavPopCallback;
import m.co.rh.id.anavigator.component.RequireNavRoute;
import m.co.rh.id.anavigator.component.RequireNavigator;
import m.co.rh.id.anavigator.component.StatefulViewFactory;
import m.co.rh.id.anavigator.exception.NavigationRouteNotFound;

/**
 * Navigator to handle navigation in an activity. Register this on Application level
 *
 * @param <ACT> Activity class type
 * @param <SV>  StatefulViewHandler type
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Navigator<ACT extends Activity, SV extends StatefulView> implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2, INavigator {
    private static final String TAG = "Navigator";
    /**
     * Key used to retrieve value from Intent data when navigator {@link #pop(Serializable)} and finishes the activity.
     * If an activity called startActivityForResult and waiting for result at onActivityResult,
     * use this to retrieve the data (intent.getSerializableExtra(ACTIVITY_RESULT_SERIALIZABLE_KEY))
     */
    public static final String ACTIVITY_RESULT_SERIALIZABLE_KEY = "onActivityResult.serializableResult";

    private final Class<ACT> mActivityClass;
    private ACT mActivity;
    private final NavConfiguration<ACT, SV> mNavConfiguration;
    private LinkedList<NavRoute> mNavRouteStack;
    private final int mViewAnimatorId;
    private boolean mIsNavigating;
    private final LinkedList<Runnable> mPendingNavigatorRoute;
    private final SnapshotHandler mNavSnapshotHandler;
    private final List<ViewNavigator> mViewNavigatorList;
    private final List<NavOnRouteChangedListener> mNavOnRouteChangedListenerList;

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
        mViewAnimatorId = Integer.MAX_VALUE;
        mPendingNavigatorRoute = new LinkedList<>();
        mNavSnapshotHandler = new SnapshotHandler(navConfiguration);
        mViewNavigatorList = new ArrayList<>();
        mNavOnRouteChangedListenerList = new ArrayList<>();
    }

    @Override
    public void push(StatefulViewFactory statefulViewFactory) {
        push(statefulViewFactory, null, null);
    }

    @Override
    public void push(StatefulViewFactory statefulViewFactory, Serializable args) {
        push(statefulViewFactory, args, null);
    }

    @Override
    public void push(StatefulViewFactory statefulViewFactory, NavPopCallback navPopCallback) {
        push(statefulViewFactory, null, navPopCallback);
    }

    @Override
    public void push(StatefulViewFactory statefulViewFactory, Serializable args, NavPopCallback navPopCallback) {
        push(statefulViewFactory, args, navPopCallback, null);
    }

    @Override
    public void push(StatefulViewFactory statefulViewFactory, Serializable args, NavPopCallback navPopCallback, RouteOptions routeOptions) {
        if (mIsNavigating) {
            // It is possible that push is invoked somewhere during initState or buildView
            // put this invocation later after previous push is done
            mPendingNavigatorRoute.add(() -> push(statefulViewFactory, args, navPopCallback));
            return;
        }
        mIsNavigating = true;
        push(statefulViewFactory, null, args, navPopCallback, routeOptions);
    }

    @Override
    public void push(String routeName, Serializable args, NavPopCallback navPopCallback, RouteOptions routeOptions) {
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
        push(statefulViewFactory, routeName, args, navPopCallback, routeOptions);
    }

    @Override
    public void push(String routeName, Serializable args, NavPopCallback navPopCallback) {
        push(routeName, args, navPopCallback, null);
    }

    @Override
    public void push(String routeName, NavPopCallback navPopCallback) {
        push(routeName, null, navPopCallback);
    }

    private void push(StatefulViewFactory statefulViewFactory, String routeName, Serializable args, NavPopCallback navPopCallback, RouteOptions routeOptions) {
        checkAndDismissDialog();
        NavRoute currentRoute = mNavRouteStack.peek();
        SV newRouteStatefulView = (SV) statefulViewFactory.newInstance(args, mActivity);
        NavRoute newRoute = new NavRoute(statefulViewFactory, navPopCallback, routeOptions, newRouteStatefulView, routeName, args, newRouteStatefulView.getKey());
        // push must be done before initState or buildView so that the newRouteStatefulView could get route information
        mNavRouteStack.push(newRoute);
        injectStatefulView(newRouteStatefulView, newRoute);
        if (newRouteStatefulView instanceof StatefulViewDialog) {
            ((StatefulViewDialog) newRouteStatefulView).showDialog(getActivity());
        } else {
            ViewAnimator existingViewAnimator = getViewAnimator();
            View view = newRouteStatefulView.buildView(mActivity, existingViewAnimator);
            existingViewAnimator.addView(view);
            existingViewAnimator.showNext();
            // try to init view navigator everytime new route is pushed
            initViewNavigator();
        }
        onRouteChanged(currentRoute, newRoute);
        mIsNavigating = false;
        if (!mPendingNavigatorRoute.isEmpty()) {
            mPendingNavigatorRoute.pop().run();
        }
        mNavSnapshotHandler.saveState(mNavRouteStack);
    }

    protected ViewAnimator getViewAnimator() {
        return mActivity.findViewById(mViewAnimatorId);
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
    public boolean pop(Serializable result) {
        if (mIsNavigating) {
            // if this pop is invoke during initState or buildView or dispose, add to pending
            mPendingNavigatorRoute.add(() -> pop(result));
            return false;
        }
        mIsNavigating = true;
        if (mNavRouteStack.size() > 1) {
            NavRoute currentRoute = mNavRouteStack.peek();
            StatefulView currentRouteStatefulView = currentRoute.getStatefulView();
            if (currentRouteStatefulView instanceof StatefulViewDialog) {
                popStack(null, result);
            } else {
                ViewAnimator existingViewAnimator = getViewAnimator();
                // check for route options
                View currentView = existingViewAnimator.getCurrentView();
                existingViewAnimator.showPrevious();
                // Try reset view navigator before pop
                resetViewNavigator(currentView);
                // cont pop
                popStack(existingViewAnimator.getCurrentView(), result);
                existingViewAnimator.removeView(currentView);
                checkAndShowDialog();
            }
            mIsNavigating = false;
            if (!mPendingNavigatorRoute.isEmpty()) {
                mPendingNavigatorRoute.pop().run();
            }
            mNavSnapshotHandler.saveState(mNavRouteStack);
            return true;
        } else {
            // Try reset view navigator before pop
            ViewAnimator existingViewAnimator = getViewAnimator();
            resetViewNavigator(existingViewAnimator.getCurrentView());
            // cont pop
            popInitialRoute(result);
            int activityResult = Activity.RESULT_CANCELED;
            if (result != null) {
                activityResult = Activity.RESULT_OK;
            }
            setActivityResultAndFinish(activityResult, result);
            mIsNavigating = false;
        }
        return false;
    }

    private void injectStatefulView(StatefulView statefulView, NavRoute navRoute) {
        if (statefulView instanceof RequireNavigator) {
            ((RequireNavigator) statefulView).provideNavigator(this);
        }
        if (statefulView instanceof RequireNavRoute) {
            ((RequireNavRoute) statefulView).provideNavRoute(navRoute);
        }
    }

    private void checkAndDismissDialog() {
        if (!mNavRouteStack.isEmpty()) {
            StatefulView currentStatefulView = mNavRouteStack.peek().getStatefulView();
            if (currentStatefulView instanceof StatefulViewDialog) {
                ((StatefulViewDialog) currentStatefulView).dismissWithoutPop(getActivity());
            }
        }
    }

    private void checkAndShowDialog() {
        if (!mNavRouteStack.isEmpty()) {
            StatefulView currentStatefulView = mNavRouteStack.peek().getStatefulView();
            if (currentStatefulView instanceof StatefulViewDialog) {
                ((StatefulViewDialog) currentStatefulView).showDialog(getActivity());
            }
        }
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

    @Override
    public void addOnRouteChangedListener(NavOnRouteChangedListener navOnRouteChangedListener) {
        mNavOnRouteChangedListenerList.add(navOnRouteChangedListener);
    }

    @Override
    public void removeOnRouteChangedListener(NavOnRouteChangedListener navOnRouteChangedListener) {
        mNavOnRouteChangedListenerList.remove(navOnRouteChangedListener);
    }

    protected void removeAllOnRouteChangedListener() {
        mNavOnRouteChangedListenerList.clear();
    }

    private void onRouteChanged(NavRoute previous, NavRoute destination) {
        if (!mNavOnRouteChangedListenerList.isEmpty()) {
            for (NavOnRouteChangedListener navOnRouteChangedListener
                    : mNavOnRouteChangedListenerList) {
                navOnRouteChangedListener.onChanged(previous, destination);
            }
        }
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
    public void retry(Serializable overrideArgs) {
        if (mIsNavigating) {
            mPendingNavigatorRoute.add(() -> retry(overrideArgs));
            return;
        }
        mIsNavigating = true;
        NavRoute currentNavRoute = mNavRouteStack.peek();
        StatefulView statefulView = currentNavRoute.getStatefulView();
        if (statefulView instanceof StatefulViewDialog) {
            StatefulViewDialog statefulViewDialog = ((StatefulViewDialog) statefulView);
            statefulViewDialog.dismissWithoutPop(getActivity());
            statefulViewDialog.dispose(getActivity());
        } else {
            statefulView.dispose(mActivity);
            ViewAnimator existingViewAnimator = getViewAnimator();
            View currentView = existingViewAnimator.getCurrentView();
            existingViewAnimator.removeView(currentView);
        }
        mNavRouteStack.pop();
        push(currentNavRoute.getStatefulViewFactory(),
                currentNavRoute.getRouteName(),
                overrideArgs,
                currentNavRoute.getNavPopCallback(),
                null);
        mIsNavigating = false;
        if (!mPendingNavigatorRoute.isEmpty()) {
            mPendingNavigatorRoute.pop().run();
        }
        mNavSnapshotHandler.saveState(mNavRouteStack);
    }

    @Override
    public void retry() {
        NavRoute currentNavRoute = mNavRouteStack.peek();
        retry(currentNavRoute.getRouteArgs());
    }

    @Override
    public void reBuildRoute(int routeIndex) {
        if (routeIndex < 0) {
            throw new NavigationRouteNotFound("Index must be 0 or more");
        }
        if (routeIndex >= mNavRouteStack.size()) {
            throw new NavigationRouteNotFound("Index exceed total route");
        }
        if (mIsNavigating) {
            mPendingNavigatorRoute.add(() -> reBuildRoute(routeIndex));
            return;
        }
        mIsNavigating = true;
        int lastIndex = mNavRouteStack.size() - 1;
        NavRoute navRoute = mNavRouteStack.get(lastIndex - routeIndex);
        StatefulView statefulView = navRoute.getStatefulView();
        if (statefulView instanceof StatefulViewDialog) {
            StatefulViewDialog statefulViewDialog = (StatefulViewDialog) statefulView;
            statefulViewDialog.dismissWithoutPop(getActivity());
            statefulViewDialog.initDialog(getActivity());
            checkAndShowDialog();
        } else {
            ViewAnimator viewAnimator = getViewAnimator();
            int selectedIndex = routeIndex;
            // check and calculate previous route, how many dialog?
            for (int i = lastIndex - routeIndex; i <= lastIndex; i++) {
                NavRoute navRoute1 = mNavRouteStack.get(i);
                if (navRoute1.getStatefulView() instanceof StatefulViewDialog) {
                    selectedIndex--;
                }
            }
            View childView = viewAnimator.getChildAt(selectedIndex);
            View currentView = viewAnimator.getCurrentView();
            View buildView = navRoute.getStatefulView().buildView(getActivity(), viewAnimator);
            viewAnimator.setInAnimation(null);
            viewAnimator.setOutAnimation(null);
            viewAnimator.removeViewAt(selectedIndex);
            viewAnimator.addView(buildView, selectedIndex);
            if (childView == currentView) {
                // animate only when current view is showing
                viewAnimator.setInAnimation(mNavConfiguration.getDefaultReBuildEnterAnimation());
                viewAnimator.setOutAnimation(mNavConfiguration.getDefaultReBuildExitAnimation());
                viewAnimator.setDisplayedChild(selectedIndex);
            }
            initViewNavigator();
        }
        mIsNavigating = false;
        if (!mPendingNavigatorRoute.isEmpty()) {
            mPendingNavigatorRoute.pop().run();
        }
    }

    @Override
    public void reBuildRoute(Pattern pattern) {
        if (pattern != null) {
            if (!mNavRouteStack.isEmpty()) {
                for (int i = mNavRouteStack.size() - 1, routeIndex = 0;
                     i >= 0;
                     i--, routeIndex++) {
                    String input = mNavRouteStack.get(i).getRouteStateKey();
                    Matcher m = pattern.matcher(input);
                    if (m.find()) {
                        reBuildRoute(routeIndex);
                    }
                }
            }
        }
    }

    @Override
    public void reBuildAllRoute() {
        if (mIsNavigating) {
            mPendingNavigatorRoute.add(() -> reBuildAllRoute());
            return;
        }
        mIsNavigating = true;
        ViewAnimator newViewAnimator = createViewAnimator(mActivity, mViewAnimatorId, mNavConfiguration);
        for (int i = mNavRouteStack.size() - 1; i >= 0; i--) {
            NavRoute navRoute = mNavRouteStack.get(i);
            StatefulView statefulView = navRoute.getStatefulView();
            if (statefulView instanceof StatefulViewDialog) {
                ((StatefulViewDialog) statefulView).initDialog(getActivity());
            } else {
                View view = statefulView.buildView(mActivity, newViewAnimator);
                newViewAnimator.addView(view);
            }
        }
        newViewAnimator.setDisplayedChild(newViewAnimator.getChildCount() - 1);
        getViewAnimator().startAnimation(mNavConfiguration.getDefaultReBuildExitAnimation());
        setViewAnimator(mActivity, newViewAnimator);
        newViewAnimator.startAnimation(mNavConfiguration.getDefaultReBuildEnterAnimation());
        checkAndDismissDialog();
        checkAndShowDialog();
        initViewNavigator();
        mIsNavigating = false;
        if (!mPendingNavigatorRoute.isEmpty()) {
            mPendingNavigatorRoute.pop().run();
        }
    }

    @Override
    public void createViewNavigator(NavConfiguration navConfiguration, int viewGroupContainerId) {
        ViewNavigator viewNavigator = new ViewNavigator(mActivityClass, navConfiguration, viewGroupContainerId);
        mViewNavigatorList.add(viewNavigator);
    }

    @Override
    public INavigator findViewNavigator(int viewGroupContainerId) {
        if (!mViewNavigatorList.isEmpty()) {
            for (ViewNavigator viewNavigator : mViewNavigatorList) {
                if (viewNavigator.getViewGroupContainerId() ==
                        viewGroupContainerId) {
                    return viewNavigator;
                }
            }
        }
        return null;
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
    public int getCurrentRouteIndex() {
        return mNavRouteStack.size() - 1;
    }

    @Override
    public void onBackPressed() {
        if (!mNavRouteStack.isEmpty()) {
            StatefulView statefulView = mNavRouteStack.peek().getStatefulView();
            if (statefulView instanceof NavOnBackPressed) {
                ViewAnimator viewAnimator = getViewAnimator();
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
                ViewAnimator viewAnimator = getViewAnimator();
                ((NavOnActivityResult) statefulView).onActivityResult(viewAnimator.getCurrentView(),
                        mActivity, this, requestCode, resultCode, data);
            }
        }
    }

    @Override
    public boolean isInitialRoute() {
        return mNavRouteStack.size() == 1;
    }

    private void popStack(View currentView, Serializable result) {
        NavRoute currentNavRoute = mNavRouteStack.pop();
        currentNavRoute.setRouteResult(result);
        StatefulView statefulView = currentNavRoute.getStatefulView();
        if (statefulView != null) {
            statefulView.dispose(mActivity);
        }
        NavPopCallback navPopCallback = currentNavRoute.getNavPopCallback();
        if (navPopCallback != null) {
            navPopCallback.onPop(mActivity, currentView, result);
        }
        onRouteChanged(currentNavRoute, mNavRouteStack.peek());
    }

    protected void popInitialRoute(Serializable result) {
        // pop initial route
        mPendingNavigatorRoute.clear();
        if (mNavRouteStack.size() == 1) {
            popStack(null, result);
        }
    }

    private ViewAnimator createViewAnimator(ACT activity, int viewAnimatorId, NavConfiguration navConfiguration) {
        ViewAnimator viewAnimator = new CustomViewAnimator(activity, this);
        viewAnimator.setId(viewAnimatorId);
        viewAnimator.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        viewAnimator.setAnimateFirstView(true);
        return viewAnimator;
    }

    protected void setViewAnimator(ACT activity, ViewAnimator viewAnimator) {
        activity.setContentView(viewAnimator);
    }

    protected void initNavigatorState(ViewAnimator viewAnimator) {
        Serializable routeStack = mNavSnapshotHandler.loadState();
        if (routeStack instanceof LinkedList) {
            mNavRouteStack = (LinkedList<NavRoute>) routeStack;
            // guard navigator in case push or pop is called inside provideNavigator
            mIsNavigating = true;
            // re-inject navigator
            for (NavRoute navRoute : mNavRouteStack) {
                StatefulView statefulView = navRoute.getStatefulView();
                injectStatefulView(statefulView, navRoute);
            }
            mIsNavigating = false;
            if (!mPendingNavigatorRoute.isEmpty()) {
                mPendingNavigatorRoute.pop().run();
            }
            Log.i(TAG, "restored navigator state");
        }
        if (!mNavRouteStack.isEmpty()) {
            // guard navigator in case push or pop is called inside buildView
            mIsNavigating = true;
            int last = mNavRouteStack.size() - 1;
            for (int i = last; i >= 0; i--) {
                NavRoute navRoute = mNavRouteStack.get(i);
                StatefulView statefulView = navRoute
                        .getStatefulView();
                if (statefulView instanceof StatefulViewDialog) {
                    ((StatefulViewDialog) statefulView).initDialog(getActivity());
                } else {
                    viewAnimator.addView(statefulView
                            .buildView(mActivity, viewAnimator)
                    );
                }
            }
            viewAnimator.setDisplayedChild(viewAnimator.getChildCount() - 1);
            mIsNavigating = false;
            if (!mPendingNavigatorRoute.isEmpty()) {
                mPendingNavigatorRoute.pop().run();
            }
        } else {
            push(mNavConfiguration.getInitialRouteName());
        }
    }

    protected Class<ACT> getActivityClass() {
        return mActivityClass;
    }

    protected int getViewAnimatorId() {
        return mViewAnimatorId;
    }

    private void initViewNavigator() {
        if (!mViewNavigatorList.isEmpty()) {
            for (ViewNavigator viewNavigator : mViewNavigatorList) {
                viewNavigator.initViewAnimator();
            }
        }
    }

    private void resetViewNavigator(View currentView) {
        if (!mViewNavigatorList.isEmpty()) {
            for (ViewNavigator viewNavigator : mViewNavigatorList) {
                viewNavigator.tryReset(currentView);
            }
        }
    }

    protected void initViewAnimator() {
        ViewAnimator viewAnimator = createViewAnimator(mActivity, mViewAnimatorId,
                mNavConfiguration);
        setViewAnimator(mActivity, viewAnimator);
        initNavigatorState(viewAnimator);
    }

    protected ACT getActivity() {
        return mActivity;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        if (mActivityClass.isInstance(activity)) {
            mActivity = (ACT) activity;
            initViewAnimator();
        }
        // handle view navigator
        if (!mViewNavigatorList.isEmpty()) {
            for (ViewNavigator viewNavigator : mViewNavigatorList) {
                viewNavigator.onActivityCreated(activity, bundle);
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
                checkAndShowDialog();
            }
            // handle view navigator
            if (!mViewNavigatorList.isEmpty()) {
                for (ViewNavigator viewNavigator : mViewNavigatorList) {
                    viewNavigator.onActivityResumed(activity);
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
                checkAndDismissDialog();
                mNavSnapshotHandler.saveState(mNavRouteStack);
            }
            // handle view navigator
            if (!mViewNavigatorList.isEmpty()) {
                for (ViewNavigator viewNavigator : mViewNavigatorList) {
                    viewNavigator.onActivityPaused(activity);
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
        if (mActivityClass.isInstance(activity)) {
            if (!activity.isFinishing()) {
                mNavSnapshotHandler.saveState(mNavRouteStack);
            }
            // handle view navigator
            if (!mViewNavigatorList.isEmpty()) {
                for (ViewNavigator viewNavigator : mViewNavigatorList) {
                    viewNavigator.onActivitySaveInstanceState(activity, bundle);
                }
            }
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (mActivityClass.isInstance(activity)) {
            if (activity.isFinishing()) {
                mNavSnapshotHandler.clearState();
                mNavSnapshotHandler.dispose();
            }
            // handle view navigator
            if (!mViewNavigatorList.isEmpty()) {
                for (ViewNavigator viewNavigator : mViewNavigatorList) {
                    viewNavigator.onActivityDestroyed(activity);
                }
            }
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
        // handle view navigator
        if (!mViewNavigatorList.isEmpty()) {
            for (ViewNavigator viewNavigator : mViewNavigatorList) {
                viewNavigator.onTrimMemory(flag);
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
        // handle view navigator
        if (!mViewNavigatorList.isEmpty()) {
            for (ViewNavigator viewNavigator : mViewNavigatorList) {
                viewNavigator.onConfigurationChanged(configuration);
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
        // handle view navigator
        if (!mViewNavigatorList.isEmpty()) {
            for (ViewNavigator viewNavigator : mViewNavigatorList) {
                viewNavigator.onLowMemory();
            }
        }
    }
}

// save state to SharedPreferences.
// during Activity.onCreate(bundle), the bundle value seemed to always null,
// and it haven't been fix until 2020, see https://issuetracker.google.com/issues/37020082
@SuppressWarnings("rawtypes")
class SnapshotHandler {
    private static final String TAG = SnapshotHandler.class.getName();

    private ExecutorService mExecutorService;
    private Future<Serializable> mStateSnapshot;
    private NavConfiguration mNavConfiguration;

    SnapshotHandler(NavConfiguration navConfiguration) {
        mNavConfiguration = navConfiguration;
        if (getFile() != null) {
            loadSnapshot(); // start load as early as possible
        }
    }

    private File getFile() {
        return mNavConfiguration.getSaveStateFile();
    }

    private Cipher getEncryptCipher() {
        return mNavConfiguration.getSaveStateEncryptCipher();
    }

    private Cipher getDecryptCipher() {
        return mNavConfiguration.getSaveStateDecryptCipher();
    }

    void saveState(Serializable serializable) {
        final File file = getFile();
        final Cipher encryptCipher = getEncryptCipher();
        if (file != null) {
            mStateSnapshot = getExecutorService().submit(() -> {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    oos.writeObject(new SealedObject(serializable, encryptCipher));
                    oos.close();
                    bos.close();
                    fileOutputStream.close();
                } catch (Throwable throwable) {
                    Log.e(TAG, "Failed to save state", throwable);
                }
                return serializable;
            });
        }
    }

    Serializable loadState() {
        if (getFile() != null) {
            if (mStateSnapshot != null) {
                return getState();
            }
            loadSnapshot();
            return getState();
        }
        return null;
    }

    private void loadSnapshot() {
        final File file = getFile();
        final Cipher decryptCipher = getDecryptCipher();
        mStateSnapshot = getExecutorService().submit(() -> {
            if (!file.exists()) {
                return null;
            }
            Serializable result = null;
            try {
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis);
                result = (Serializable)
                        ((SealedObject) ois.readObject()).getObject(decryptCipher);
                ois.close();
                bis.close();
                fis.close();
            } catch (Throwable throwable) {
                Log.e(TAG, "Failed to load snapshot", throwable);
            }
            return result;
        });
    }

    void clearState() {
        if (mStateSnapshot != null) {
            mStateSnapshot.cancel(false);
            mStateSnapshot = null;
        }
        final File file = getFile();
        if (file != null) {
            getExecutorService().submit(() -> {
                file.delete();
            });
        }
    }

    private ExecutorService getExecutorService() {
        if (mExecutorService == null) {
            mExecutorService = Executors.newSingleThreadExecutor();
        }
        return mExecutorService;
    }

    private Serializable getState() {
        if (mStateSnapshot != null) {
            try {
                // prevent ANR
                while (!mStateSnapshot.isDone()) {
                    Thread.yield();
                }
                return mStateSnapshot.get();
            } catch (Exception e) {
                Log.e("getState", "Unable to get snapshot", e);
            }
        }
        return null;
    }

    void dispose() {
        if (mExecutorService != null) {
            mExecutorService.shutdown();
            mExecutorService = null;
        }
        if (mStateSnapshot != null) {
            mStateSnapshot.cancel(false);
            mStateSnapshot = null;
        }
    }
}

/**
 * Custom view animator that automatically switch the inAnimation and outAnimation when showPrevious or showNext
 */
class CustomViewAnimator extends ViewAnimator {

    private Navigator mNavigator;

    public CustomViewAnimator(Context context, Navigator navigator) {
        super(context);
        mNavigator = navigator;
    }

    public CustomViewAnimator(Context context, AttributeSet attrs, Navigator navigator) {
        super(context, attrs);
        mNavigator = navigator;
    }

    @Override
    public void showPrevious() {
        NavConfiguration navConfiguration = mNavigator.getNavConfiguration();
        NavRoute currentRoute = mNavigator.getCurrentRoute();
        Animation inAnimation = navConfiguration.getDefaultPopEnterAnimation();
        Animation outAnimation = navConfiguration.getDefaultPopExitAnimation();
        if (currentRoute != null) {
            RouteOptions routeOptions = currentRoute.getRouteOptions();
            if (routeOptions != null) {
                Integer inAnimationId = routeOptions.getPopEnterAnimationResId();
                if (inAnimationId != null) {
                    try {
                        inAnimation = AnimationUtils.loadAnimation(getContext(),
                                inAnimationId);
                    } catch (Throwable t) {
                        inAnimation = null;
                    }
                } else {
                    inAnimation = null;
                }
                Integer outAnimationId = routeOptions.getPopExitAnimationResId();
                if (outAnimationId != null) {
                    try {
                        outAnimation = AnimationUtils.loadAnimation(getContext(),
                                outAnimationId);
                    } catch (Throwable t) {
                        outAnimation = null;
                    }
                } else {
                    outAnimation = null;
                }
            }
        }
        setInAnimation(inAnimation);
        setOutAnimation(outAnimation);
        super.showPrevious();
    }

    @Override
    public void showNext() {
        NavConfiguration navConfiguration = mNavigator.getNavConfiguration();
        NavRoute currentRoute = mNavigator.getCurrentRoute();
        Animation inAnimation = navConfiguration.getDefaultEnterAnimation();
        Animation outAnimation = navConfiguration.getDefaultExitAnimation();
        if (currentRoute != null) {
            RouteOptions routeOptions = mNavigator.getCurrentRoute().getRouteOptions();
            if (routeOptions != null) {
                Integer inAnimationId = routeOptions.getEnterAnimationResId();
                if (inAnimationId != null) {
                    try {
                        inAnimation = AnimationUtils.loadAnimation(getContext(),
                                inAnimationId);
                    } catch (Throwable t) {
                        inAnimation = null;
                    }
                } else {
                    inAnimation = null;
                }
                Integer outAnimationId = routeOptions.getExitAnimationResId();
                if (outAnimationId != null) {
                    try {
                        outAnimation = AnimationUtils.loadAnimation(getContext(),
                                outAnimationId);
                    } catch (Throwable t) {
                        outAnimation = null;
                    }
                } else {
                    outAnimation = null;
                }
            }
        }
        setInAnimation(inAnimation);
        setOutAnimation(outAnimation);
        super.showNext();
    }
}