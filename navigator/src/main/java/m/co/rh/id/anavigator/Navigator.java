package m.co.rh.id.anavigator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;

import m.co.rh.id.anavigator.annotation.NavInject;
import m.co.rh.id.anavigator.annotation.NavRouteIndex;
import m.co.rh.id.anavigator.annotation.NavViewNavigator;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.NavActivityLifecycle;
import m.co.rh.id.anavigator.component.NavComponentCallback;
import m.co.rh.id.anavigator.component.NavOnActivityResult;
import m.co.rh.id.anavigator.component.NavOnBackPressed;
import m.co.rh.id.anavigator.component.NavOnRequestPermissionResult;
import m.co.rh.id.anavigator.component.NavOnRouteChangedListener;
import m.co.rh.id.anavigator.component.NavPopCallback;
import m.co.rh.id.anavigator.component.RequireComponent;
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
    private final ThreadPoolExecutor mThreadPool;
    private final Handler mHandler;

    /**
     * @param activityClass activity class that this navigator handles
     */
    public Navigator(Class<ACT> activityClass, NavConfiguration<ACT, SV> navConfiguration) {
        if (activityClass == null) {
            throw new IllegalStateException("activityClass must not null");
        }
        if (navConfiguration == null) {
            throw new IllegalStateException("navConfiguration must not null");
        }
        mActivityClass = activityClass;
        mNavConfiguration = navConfiguration;
        mNavRouteStack = new LinkedList<>();
        mViewAnimatorId = Integer.MAX_VALUE;
        mPendingNavigatorRoute = new LinkedList<>();
        mViewNavigatorList = new ArrayList<>();
        mNavOnRouteChangedListenerList = new ArrayList<>();
        mThreadPool = mNavConfiguration.getThreadPoolExecutor();
        mHandler = mNavConfiguration.getMainHandler();
        mNavSnapshotHandler = new SnapshotHandler(navConfiguration);
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
        return popInternal(result, true);
    }

    private boolean popInternal(Serializable result, boolean triggerCheckAndShowDialog) {
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
            }
            if (triggerCheckAndShowDialog) {
                checkAndShowDialog();
            }
            mIsNavigating = false;
            if (!mPendingNavigatorRoute.isEmpty()) {
                mPendingNavigatorRoute.pop().run();
            }
            mNavSnapshotHandler.saveState(mNavRouteStack);
            if (isInitialRoute()) {
                System.gc();
            }
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
        boolean isAnnotationInjection = mNavConfiguration.isEnableAnnotationInjection();
        List<Field> fieldList = new ArrayList<>();
        if (isAnnotationInjection) {
            fieldList = Arrays.asList(statefulView.getClass().getDeclaredFields());
        }
        boolean isFieldNotEmpty = !fieldList.isEmpty();
        if (statefulView instanceof RequireNavigator) {
            ((RequireNavigator) statefulView).provideNavigator(this);
        }
        if (isAnnotationInjection && isFieldNotEmpty) {
            List<Future> futures = new ArrayList<>();
            for (Field field : fieldList) {
                futures.add(mThreadPool.submit(() -> navInjectNavigator(statefulView, field)));
                futures.add(mThreadPool.submit(() ->
                        navViewNavigator(statefulView, navRoute, field)));
            }
            waitFutures(futures);
        }
        if (statefulView instanceof RequireNavRoute) {
            ((RequireNavRoute) statefulView).provideNavRoute(navRoute);
        }
        if (isAnnotationInjection && isFieldNotEmpty) {
            List<Future> futures = new ArrayList<>();
            for (Field field : fieldList) {
                futures.add(mThreadPool.submit(() -> navInjectNavRoute(statefulView, navRoute, field)));
                futures.add(mThreadPool.submit(() -> navRouteIndex(statefulView, navRoute, field)));
            }
            waitFutures(futures);
        }
        if (statefulView instanceof RequireComponent) {
            ((RequireComponent) statefulView)
                    .provideComponent(mNavConfiguration.getRequiredComponent());
        }
        if (isAnnotationInjection && isFieldNotEmpty) {
            List<Future> futures = new ArrayList<>();
            for (Field field : fieldList) {
                futures.add(mThreadPool.submit(() -> navInjectRequiredComponent(statefulView, field)));
                futures.add(mThreadPool.submit(() ->
                        navInjectStatefulViews(statefulView, navRoute, field)));
            }
            waitFutures(futures);
        }
    }

    private void waitFutures(List<Future> futures) {
        while (!futures.isEmpty()) {
            boolean allDone = true;
            for (Future future : futures) {
                if (!future.isDone()) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) {
                futures.clear();
            } else {
                // deadlock gonna happen if max thread is 1,
                // AND when injecting nested StatefulView.
                // deadlock gonna happen as well if max thread is overwhelmed
                // by nested StatefulView.
                // So, if not yet done, steal task to avoid deadlock.
                try {
                    Runnable runnable = mThreadPool.getQueue().poll();
                    if (runnable != null) {
                        runnable.run();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error executing stolen task while injecting");
                }
            }
        }
    }

    private void navViewNavigator(StatefulView statefulView, NavRoute navRoute, Field field) {
        NavViewNavigator navViewNavigator = field.getAnnotation(NavViewNavigator.class);
        if (navViewNavigator == null) {
            return;
        }
        int containerId = getActivity().getResources().getIdentifier(navViewNavigator.value(), "id",
                getActivity().getPackageName());
        INavigator viewNavigator = findViewNavigator(containerId);
        if (viewNavigator != null) {
            Class fieldType = field.getType();
            String errorMessage = "Failed to inject " + fieldType.getName() + " " + field.getName();
            if (INavigator.class.isAssignableFrom(fieldType)) {
                Log.v(TAG, "navViewNavigator injected: " + fieldType.getName() + " " + field.getName());
                field.setAccessible(true);
                try {
                    field.set(statefulView, viewNavigator);
                } catch (IllegalAccessException e) {
                    Log.e(TAG, errorMessage, e);
                } finally {
                    field.setAccessible(false);
                }
            }
        } else {
            Log.v(TAG, "navViewNavigator not injected: due to viewNavigator is null"
                    + " containerId?" + containerId);
        }
    }

    private void navRouteIndex(StatefulView statefulView, NavRoute navRoute, Field field) {
        NavRouteIndex navRouteIndex = field.getAnnotation(NavRouteIndex.class);
        if (navRouteIndex != null) {
            Class fieldType = field.getType();
            String errorMessage = "Failed to inject " + fieldType.getName() + " " + field.getName();
            if (Number.class.isAssignableFrom(fieldType)) {
                Log.v(TAG, "navInjectRouteIndex injected: " + fieldType.getName() + " " + field.getName());
                field.setAccessible(true);
                try {
                    int routeIndex = findRouteIndex(navRoute);
                    if (Byte.class.isAssignableFrom(fieldType)) {
                        field.set(statefulView, (byte) routeIndex);
                    } else if (Short.class.isAssignableFrom(fieldType)) {
                        field.set(statefulView, (short) routeIndex);
                    } else if (Integer.class.isAssignableFrom(fieldType)) {
                        field.set(statefulView, routeIndex);
                    } else if (Long.class.isAssignableFrom(fieldType)) {
                        field.set(statefulView, (long) routeIndex);
                    } else if (Float.class.isAssignableFrom(fieldType)) {
                        field.set(statefulView, (float) routeIndex);
                    } else if (Double.class.isAssignableFrom(fieldType)) {
                        field.set(statefulView, (double) routeIndex);
                    }
                } catch (IllegalAccessException e) {
                    Log.e(TAG, errorMessage, e);
                } finally {
                    field.setAccessible(false);
                }
            } else if (fieldType.isPrimitive() && !boolean.class.isAssignableFrom(fieldType)) {
                Log.v(TAG, "navInjectRouteIndex injected: " + fieldType.getName() + " " + field.getName());
                field.setAccessible(true);
                try {
                    int routeIndex = findRouteIndex(navRoute);
                    if (byte.class.isAssignableFrom(fieldType)) {
                        field.setByte(statefulView, (byte) routeIndex);
                    } else if (short.class.isAssignableFrom(fieldType)) {
                        field.setShort(statefulView, (short) routeIndex);
                    } else if (int.class.isAssignableFrom(fieldType)) {
                        field.setInt(statefulView, routeIndex);
                    } else if (long.class.isAssignableFrom(fieldType)) {
                        field.setLong(statefulView, routeIndex);
                    } else if (float.class.isAssignableFrom(fieldType)) {
                        field.setFloat(statefulView, (float) routeIndex);
                    } else if (double.class.isAssignableFrom(fieldType)) {
                        field.setDouble(statefulView, routeIndex);
                    }
                } catch (IllegalAccessException e) {
                    Log.e(TAG, errorMessage, e);
                } finally {
                    field.setAccessible(false);
                }
            }
        }
    }

    private void navInjectNavigator(StatefulView statefulView, Field field) {
        NavInject navInject = field.getAnnotation(NavInject.class);
        if (navInject != null) {
            Class fieldType = field.getType();
            String errorMessage = "Failed to inject " + fieldType.getName() + " " + field.getName();
            if (fieldType.isAssignableFrom(INavigator.class)) {
                Log.v(TAG, "navigator injected: " + fieldType.getName() + " " + field.getName());
                field.setAccessible(true);
                try {
                    field.set(statefulView, this);
                } catch (IllegalAccessException e) {
                    Log.e(TAG, errorMessage, e);
                } finally {
                    field.setAccessible(false);
                }
            }
        }
    }

    private void navInjectNavRoute(StatefulView statefulView, NavRoute navRoute, Field field) {
        NavInject navInject = field.getAnnotation(NavInject.class);
        if (navInject != null) {
            Class fieldType = field.getType();
            String errorMessage = "Failed to inject " + fieldType.getName() + " " + field.getName();
            if (fieldType.isAssignableFrom(NavRoute.class)) {
                Log.v(TAG, "navRoute injected: " + fieldType.getName() + " " + field.getName());
                if (navRoute != null) {
                    field.setAccessible(true);
                    try {
                        field.set(statefulView, navRoute);
                    } catch (IllegalAccessException e) {
                        Log.e(TAG, errorMessage, e);
                    } finally {
                        field.setAccessible(false);
                    }
                }
            }
        }
    }

    private void navInjectRequiredComponent(StatefulView statefulView, Field field) {
        NavInject navInject = field.getAnnotation(NavInject.class);
        if (navInject != null) {
            Class fieldType = field.getType();
            String errorMessage = "Failed to inject " + fieldType.getName() + " " + field.getName();
            if (mNavConfiguration.getRequiredComponent() != null && fieldType.isAssignableFrom(mNavConfiguration.getRequiredComponent().getClass())) {
                Log.v(TAG, "requiredComponent injected: " + fieldType.getName() + " " + field.getName());
                field.setAccessible(true);
                try {
                    field.set(statefulView, mNavConfiguration.getRequiredComponent());
                } catch (IllegalAccessException e) {
                    Log.e(TAG, errorMessage, e);
                } finally {
                    field.setAccessible(false);
                }
            }
        }
    }

    private void navInjectStatefulViews(StatefulView statefulView, NavRoute navRoute, Field field) {
        NavInject navInject = field.getAnnotation(NavInject.class);
        if (navInject != null) {
            Class fieldType = field.getType();
            String errorMessage = "Failed to inject " + fieldType.getName() + " " + field.getName();
            if (StatefulView.class.isAssignableFrom(fieldType)) {
                field.setAccessible(true);
                try {
                    Object object = field.get(statefulView);
                    if (object instanceof StatefulView) {
                        Log.v(TAG, "statefulView injected: " + fieldType.getName() + " " + field.getName());
                        injectStatefulView((StatefulView) object, navRoute);
                    }
                } catch (IllegalAccessException e) {
                    Log.e(TAG, errorMessage, e);
                } finally {
                    field.setAccessible(false);
                }
            } else if (Iterable.class.isAssignableFrom(fieldType)) {
                field.setAccessible(true);
                try {
                    Object object = field.get(statefulView);
                    Log.v(TAG, "trying inject iterable");
                    if (object instanceof Iterable) {
                        Iterable iterable = (Iterable) object;
                        for (Object statefulViewObj : iterable
                        ) {
                            if (statefulViewObj instanceof StatefulView) {
                                Log.v(TAG, "iterable injected: " + statefulViewObj);
                                injectStatefulView((StatefulView) statefulViewObj, navRoute);
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    Log.e(TAG, errorMessage, e);
                } finally {
                    field.setAccessible(false);
                }
            }
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
        dispose();
        setActivityResultAndFinish(Activity.RESULT_OK, result);
    }

    @Override
    public void finishActivity() {
        finishActivity(null);
    }

    void dispose() {
        // dispose view navigator first
        if (!mViewNavigatorList.isEmpty()) {
            for (ViewNavigator viewNavigator : mViewNavigatorList) {
                viewNavigator.dispose();
            }
        }
        // dispose every stack and clear it
        while (!mNavRouteStack.isEmpty()) {
            StatefulView statefulView = mNavRouteStack.pop().getStatefulView();
            statefulView.dispose(mActivity);
        }
        mPendingNavigatorRoute.clear();
        mIsNavigating = false;
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
    public void popUntil(String routeName, Serializable result) {
        if (routeName == null) throw new NavigationRouteNotFound("Route name is null");
        if (!mNavRouteStack.isEmpty()) {
            NavRoute navRouteUntil = null;
            for (NavRoute navRoute : mNavRouteStack) {
                if (routeName.equals(navRoute.getRouteName())) {
                    navRouteUntil = navRoute;
                    break;
                }
            }
            if (navRouteUntil == null) {
                throw new NavigationRouteNotFound("Route not found");
            }

            NavRoute currentNavRoute = mNavRouteStack.peek();
            while (currentNavRoute != navRouteUntil) {
                popInternal(result, false);
                currentNavRoute = mNavRouteStack.peek();
            }
        }
    }

    @Override
    public void popUntil(String routeName) {
        popUntil(routeName, null);
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
            int selectedIndex = calculateRouteIndexForViewAnimator(routeIndex);
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

    // get the actual route index for ViewAnimator only
    private int calculateRouteIndexForViewAnimator(int routeIndex) {
        int lastIndex = mNavRouteStack.size() - 1;
        int selectedIndex = routeIndex;
        // check and calculate previous route, how many dialog?
        for (int i = lastIndex - routeIndex; i <= lastIndex; i++) {
            NavRoute navRoute1 = mNavRouteStack.get(i);
            if (navRoute1.getStatefulView() instanceof StatefulViewDialog) {
                selectedIndex--;
            }
        }
        return selectedIndex;
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
            mPendingNavigatorRoute.add(this::reBuildAllRoute);
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
    public void injectRequired(StatefulView parentStatefulView, StatefulView... statefulViews) {
        if (parentStatefulView == null) {
            Log.e(TAG, "Parent StatefulView is required");
            return;
        }
        if (statefulViews != null) {
            NavRoute injectedNavRoute = null;
            if (!mNavRouteStack.isEmpty()) {
                for (NavRoute navRoute : mNavRouteStack) {
                    if (parentStatefulView == navRoute.getStatefulView()) {
                        injectedNavRoute = navRoute;
                        break;
                    }
                }
            }

            for (StatefulView statefulView : statefulViews) {
                injectStatefulView(statefulView, injectedNavRoute);
            }
        }
    }

    @Override
    public int findRouteIndex(NavRoute navRoute) {
        int result = -1;
        if (navRoute != null) {
            if (!mNavRouteStack.isEmpty()) {
                for (int i = mNavRouteStack.size() - 1, routeIndex = 0;
                     i >= 0;
                     i--, routeIndex++) {
                    if (mNavRouteStack.get(i) == navRoute) {
                        result = routeIndex;
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public View findView(NavRoute navRoute) {
        View result = null;
        if (navRoute != null) {
            if (!(navRoute.getStatefulView() instanceof StatefulViewDialog)) {
                int routeIndex = findRouteIndex(navRoute);
                if (routeIndex != -1) {
                    result = getViewAnimator().getChildAt(
                            calculateRouteIndexForViewAnimator(routeIndex)
                    );
                }
            }
        }
        return result;
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
                NavOnActivityResult navOnActivityResult = (NavOnActivityResult) statefulView;
                if (statefulView instanceof StatefulViewDialog) {
                    // must be done in another loop post
                    /* Android dialog defer show and dismiss execution into another loop,
                     * so this must be posted in the next loop
                     * after dialog has been shown to avoid inconsistent state
                     */
                    mHandler.post(() -> navOnActivityResult.onActivityResult(null,
                            mActivity, this, requestCode, resultCode, data));
                } else {
                    navOnActivityResult.onActivityResult(getViewAnimator().getCurrentView(),
                            mActivity, this, requestCode, resultCode, data);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (!mNavRouteStack.isEmpty()) {
            StatefulView statefulView = mNavRouteStack.peek().getStatefulView();
            if (statefulView instanceof NavOnRequestPermissionResult) {
                View currentView;
                if (statefulView instanceof StatefulViewDialog) {
                    currentView = null;
                } else {
                    currentView = getViewAnimator().getCurrentView();
                }
                ((NavOnRequestPermissionResult) statefulView).onRequestPermissionsResult(currentView,
                        mActivity, this, requestCode, permissions, grantResults);
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
            navPopCallback.onPop(this, currentNavRoute, mActivity, currentView);
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
        // prepare loading view
        View loadingView = mNavConfiguration.getLoadingView();
        if (loadingView != null) {
            viewAnimator.addView(loadingView);
        }
        mThreadPool.execute(() -> {
            Serializable routeStack = mNavSnapshotHandler.loadState();
            mHandler.post(() -> {
                // remove loading view
                if (loadingView != null) {
                    viewAnimator.removeView(loadingView);
                }
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
            });
        });
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

    @Override
    public ACT getActivity() {
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
                checkAndShowDialog();
                for (NavRoute navRoute : mNavRouteStack) {
                    StatefulView statefulView = navRoute.getStatefulView();
                    if (statefulView instanceof NavActivityLifecycle) {
                        Runnable onActivityResume = () -> ((NavActivityLifecycle) statefulView).onNavActivityResumed(mActivity);
                        if (statefulView instanceof StatefulViewDialog) {
                            mHandler.post(onActivityResume);
                        } else {
                            onActivityResume.run();
                        }
                    }
                }
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
                        ((NavActivityLifecycle) statefulView).onNavActivityPaused(mActivity);
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
        System.gc();
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

// during Activity.onCreate(bundle), the bundle value seemed to always null,
// and it haven't been fix until 2020, see https://issuetracker.google.com/issues/37020082
@SuppressWarnings("rawtypes")
class SnapshotHandler {
    private static final String TAG = SnapshotHandler.class.getName();

    private ThreadPoolExecutor mThreadPool;
    private Future<Serializable> mStateSnapshot;
    private NavConfiguration mNavConfiguration;

    SnapshotHandler(NavConfiguration navConfiguration) {
        mNavConfiguration = navConfiguration;
        mThreadPool = navConfiguration.getThreadPoolExecutor();
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
            getExecutorService().submit((Runnable) file::delete);
        }
    }

    private ExecutorService getExecutorService() {
        return mThreadPool;
    }

    private Serializable getState() {
        if (mStateSnapshot != null) {
            try {
                return mStateSnapshot.get();
            } catch (Exception e) {
                Log.e(TAG, "Unable to get snapshot", e);
            }
        }
        return null;
    }

    void dispose() {
        if (mStateSnapshot != null) {
            mStateSnapshot.cancel(false);
            mStateSnapshot = null;
        }
    }
}

/**
 * Custom view animator that automatically switch the inAnimation and outAnimation when showPrevious or showNext
 */
@SuppressLint("ViewConstructor")
@SuppressWarnings("rawtypes")
class CustomViewAnimator extends ViewAnimator {

    private final Navigator mNavigator;

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