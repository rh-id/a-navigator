package m.co.rh.id.anavigator;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import androidx.transition.Scene;
import androidx.transition.Transition;
import androidx.transition.TransitionInflater;
import androidx.transition.TransitionManager;

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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import m.co.rh.id.anavigator.component.NavActivity;
import m.co.rh.id.anavigator.component.NavActivityLifecycle;
import m.co.rh.id.anavigator.component.NavBackEvent;
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
    /**
     * True between {@link #onBackStarted(NavBackEvent)} and the gesture
     * outcome (commit through {@link #onBackPressed()} or
     * {@link #onBackCancelled()}). While active, the gesture-driven pop
     * animation may run inside {@link NavViewLayout} (its state is tracked
     * separately there) and the next {@link #onBackPressed()} invocation is
     * interpreted as the gesture commit.
     */
    private boolean mPredictiveBackActive;
    private final LinkedList<Runnable> mPendingNavigatorRoute;
    private final SnapshotHandler mNavSnapshotHandler;
    private final List<ViewNavigator> mViewNavigatorList;
    private final List<NavOnRouteChangedListener> mNavOnRouteChangedListenerList;
    private final ThreadPoolExecutor mThreadPool;
    private final Handler mHandler;
    private int mActivityDefaultScreenOrientation;

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
        // a queued execution of this method (see above) re-enters here once the
        // running navigation released mIsNavigating, hence cancelling here also
        // covers the queued-later case
        cancelOngoingPredictiveBack();
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
        // a queued execution of this method (see above) re-enters here once the
        // running navigation released mIsNavigating, hence cancelling here also
        // covers the queued-later case
        cancelOngoingPredictiveBack();
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
        NavRoute currentRoute = mNavRouteStack.peek();
        NavRoute newRoute = doPush(statefulViewFactory, routeName, args, navPopCallback, routeOptions);
        finishNavigation(currentRoute, newRoute);
    }

    /**
     * Core of a push: mutates the route stack and swaps the displayed view only.
     * It does NOT fire any route-changed event, does NOT release {@link #mIsNavigating},
     * does NOT drain {@link #mPendingNavigatorRoute} and does NOT save any state.
     * Always call {@link #finishNavigation(NavRoute, NavRoute)} afterwards to
     * complete the navigation. This split allows callers such as
     * {@link #replace(String, Serializable, NavPopCallback, RouteOptions)} and
     * {@link #retry(Serializable)} to run view-stack cleanup between the view
     * swap and the pending-routes drain/state save.
     */
    private NavRoute doPush(StatefulViewFactory statefulViewFactory, String routeName, Serializable args, NavPopCallback navPopCallback, RouteOptions routeOptions) {
        checkAndDismissDialog();
        SV newRouteStatefulView = (SV) statefulViewFactory.newInstance(args, mActivity);
        NavRoute newRoute = new NavRoute(statefulViewFactory, navPopCallback, routeOptions, newRouteStatefulView, routeName, args, newRouteStatefulView.getKey());
        // push must be done before initState or buildView so that the newRouteStatefulView could get route information
        mNavRouteStack.push(newRoute);
        injectStatefulView(newRouteStatefulView, newRoute);
        if (newRouteStatefulView instanceof StatefulViewDialog) {
            ((StatefulViewDialog) newRouteStatefulView).showDialog(getActivity());
        } else {
            NavViewLayout existingViewAnimator = getViewAnimator();
            View view = newRouteStatefulView.buildView(mActivity, existingViewAnimator);
            existingViewAnimator.addToStack(view);
            existingViewAnimator.showNext();
            // try to init view navigator everytime new route is pushed
            initViewNavigator();
        }
        return newRoute;
    }

    /**
     * Tail of a navigation: fires the route-changed events, updates the requested
     * orientation, releases {@link #mIsNavigating}, drains any navigation queued
     * in {@link #mPendingNavigatorRoute} while {@link #mIsNavigating} was true and
     * finally saves the state exactly once (the route stack is already final here).
     */
    private void finishNavigation(NavRoute previousRoute, NavRoute newRoute) {
        onRouteChanged(previousRoute, newRoute);
        checkAndConfigureRequestOrientation();
        mIsNavigating = false;
        if (!mPendingNavigatorRoute.isEmpty()) {
            mPendingNavigatorRoute.pop().run();
        }
        mNavSnapshotHandler.saveState(mNavRouteStack);
    }

    protected NavViewLayout getViewAnimator() {
        if (mActivity == null) {
            return null;
        }
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
        return popInternal(result);
    }

    private boolean popInternal(Serializable result) {
        return popInternal(result, true, true);
    }

    private boolean popInternal(Serializable result, boolean triggerCheckAndShowDialog) {
        return popInternal(result, triggerCheckAndShowDialog, true);
    }

    private boolean popInternal(Serializable result, boolean triggerCheckAndShowDialog, boolean exitWhenStackEmpty) {
        return popInternal(result, triggerCheckAndShowDialog, exitWhenStackEmpty, false);
    }

    /**
     * Same as {@link #popInternal(Serializable, boolean, boolean)} with
     * control over the standard pop view transition.
     *
     * @param suppressPopTransition when true, the standard
     *                              {@link NavViewLayout#showPrevious()} swap is
     *                              replaced by
     *                              {@link NavViewLayout#finishPredictiveBack()}
     *                              which plays the predictive back gesture
     *                              commit animation instead (the previous page
     *                              is already attached beneath the animating
     *                              current page). Only used by the predictive
     *                              back gesture commit path
     */
    private boolean popInternal(Serializable result, boolean triggerCheckAndShowDialog, boolean exitWhenStackEmpty, boolean suppressPopTransition) {
        if (mIsNavigating) {
            // if this pop is invoke during initState or buildView or dispose, add to pending
            mPendingNavigatorRoute.add(() -> pop(result));
            return false;
        }
        // NOTE: the predictive back commit path clears mPredictiveBackActive
        // BEFORE calling this method with suppressPopTransition=true, so this
        // cancellation never fights the commit it is about to perform.
        cancelOngoingPredictiveBack();
        mIsNavigating = true;
        if (mNavRouteStack.size() > 1) {
            NavRoute currentRoute = mNavRouteStack.peek();
            StatefulView currentRouteStatefulView = currentRoute.getStatefulView();
            if (currentRouteStatefulView instanceof StatefulViewDialog) {
                popStack(null, result);
            } else {
                NavViewLayout existingViewAnimator = getViewAnimator();
                // check for route options
                View currentView = existingViewAnimator.getCurrentView();
                if (suppressPopTransition) {
                    existingViewAnimator.finishPredictiveBack();
                } else {
                    existingViewAnimator.showPrevious();
                }
                // Try reset view navigator before pop
                resetViewNavigator(currentView);
                // cont pop
                popStack(currentView, result);
                existingViewAnimator.removeFromStack(currentView);
            }
            if (triggerCheckAndShowDialog) {
                checkAndShowDialog();
            }
            checkAndConfigureRequestOrientation();
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
            NavViewLayout existingViewAnimator = getViewAnimator();
            resetViewNavigator(existingViewAnimator.getCurrentView());
            // cont pop
            popInitialRoute(result);
            if (exitWhenStackEmpty) {
                int activityResult = Activity.RESULT_CANCELED;
                if (result != null) {
                    activityResult = Activity.RESULT_OK;
                }
                setActivityResultAndFinish(activityResult, result);
            }
            mIsNavigating = false;
        }
        return false;
    }

    private void injectStatefulView(StatefulView statefulView, NavRoute navRoute) {
        boolean isAnnotationInjection = mNavConfiguration.isEnableAnnotationInjection();
        List<Field> fieldList = new ArrayList<>();
        if (isAnnotationInjection) {
            Class<?> clazz = statefulView.getClass();
            while (clazz != null && clazz != Object.class) {
                fieldList.addAll(Arrays.asList(clazz.getDeclaredFields()));
                clazz = clazz.getSuperclass();
            }
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
        // cancel any in-flight predictive back gesture first: dispose pops
        // every route, the gesture animation must not outlive the views it
        // borrows
        cancelOngoingPredictiveBack();
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
    public void replace(String routeName, Serializable args, NavPopCallback navPopCallback, RouteOptions routeOptions) {
        if (mIsNavigating) {
            // It is possible that replace is invoked somewhere during initState or buildView
            // put this invocation later after previous navigation is done
            mPendingNavigatorRoute.add(() -> replace(routeName, args, navPopCallback, routeOptions));
            return;
        }
        cancelOngoingPredictiveBack();
        if (mNavRouteStack.isEmpty()) {
            // nothing to replace, fallback to push
            push(routeName, args, navPopCallback, routeOptions);
            return;
        }
        // resolve the target factory BEFORE any mutation: an unknown route must
        // throw without popping/disposing the current route or touching the view
        StatefulViewFactory<ACT, SV> statefulViewFactory = mNavConfiguration.getNavMap().get(routeName);
        if (statefulViewFactory == null) {
            throw new NavigationRouteNotFound(routeName + " not found");
        }
        mIsNavigating = true;
        NavRoute oldRoute = mNavRouteStack.peek();
        StatefulView oldRouteStatefulView = oldRoute.getStatefulView();
        View oldView = null;
        if (oldRouteStatefulView instanceof StatefulViewDialog) {
            ((StatefulViewDialog) oldRouteStatefulView).dismissWithoutPop(getActivity());
            // pop the dialog route: disposes it, triggers onPop(null) callback
            // and fires onRouteChanged(oldRoute -> route below it)
            popStack(null, null);
        } else {
            NavViewLayout existingViewAnimator = getViewAnimator();
            // keep the old view attached and inside the view stack for now,
            // showNext() inside doPush needs it to compute the display index.
            // It is removed from the view stack right after doPush.
            oldView = existingViewAnimator.getCurrentView();
            // try reset view navigator before pop
            resetViewNavigator(oldView);
            // pop the route only (no showPrevious here): disposes it, triggers
            // onPop(currentView) callback and fires onRouteChanged(oldRoute -> route below it).
            // The displayed view is swapped exactly once by the single
            // showNext() inside doPush, this keeps the view stack in sync
            // with the route stack for every RouteOptions type.
            popStack(oldView, null);
        }
        // route below the popped one, needed for the route-changed event pair
        NavRoute underRoute = mNavRouteStack.peek();
        NavRoute newRoute = doPush(statefulViewFactory, routeName, args, navPopCallback, routeOptions);
        if (oldView != null) {
            NavViewLayout existingViewAnimator = getViewAnimator();
            if (newRoute.getStatefulView() instanceof StatefulViewDialog) {
                // the dialog branch of doPush does not swap the displayed
                // child: show the view of the route below the replaced one,
                // same as what the popped route leaves behind. When the
                // replaced route was the initial one there is no route below,
                // the old (already disposed) view stays attached like the
                // previous implementation did.
                int underViewIndex = existingViewAnimator.indexInStack(oldView) - 1;
                if (underViewIndex >= 0) {
                    existingViewAnimator.animateTo(underViewIndex);
                }
            }
            // remove the replaced view from the view stack before draining
            // pending routes and saving the state, the layout child itself was
            // already swapped by showNext() inside doPush for a non-dialog
            // destination, or by animateTo() above for a dialog destination
            existingViewAnimator.removeFromStack(oldView);
        }
        finishNavigation(underRoute, newRoute);
    }

    @Override
    public void replace(String routeName, Serializable args, NavPopCallback navPopCallback) {
        replace(routeName, args, navPopCallback, null);
    }

    @Override
    public void replace(String routeName, NavPopCallback navPopCallback) {
        replace(routeName, null, navPopCallback, null);
    }

    @Override
    public void replace(String routeName, Serializable args) {
        replace(routeName, args, null, null);
    }

    @Override
    public void replace(String routeName) {
        replace(routeName, null, null, null);
    }

    @Override
    public void retry(Serializable overrideArgs) {
        if (mIsNavigating) {
            mPendingNavigatorRoute.add(() -> retry(overrideArgs));
            return;
        }
        if (mNavRouteStack.isEmpty()) {
            return;
        }
        cancelOngoingPredictiveBack();
        mIsNavigating = true;
        NavRoute currentNavRoute = mNavRouteStack.peek();
        StatefulView statefulView = currentNavRoute.getStatefulView();
        View oldView = null;
        if (statefulView instanceof StatefulViewDialog) {
            StatefulViewDialog statefulViewDialog = ((StatefulViewDialog) statefulView);
            statefulViewDialog.dismissWithoutPop(getActivity());
            statefulViewDialog.dispose(getActivity());
        } else {
            NavViewLayout existingViewAnimator = getViewAnimator();
            // keep the old view attached and inside the view stack for now,
            // showNext() inside doPush needs it to compute the display index.
            // It is removed from the view stack right after doPush.
            oldView = existingViewAnimator.getCurrentView();
            // try reset view navigator before rebuild
            resetViewNavigator(oldView);
            statefulView.dispose(mActivity);
        }
        mNavRouteStack.pop();
        // route below the popped one, needed for the route-changed event
        NavRoute underRoute = mNavRouteStack.peek();
        NavRoute newRoute = doPush(currentNavRoute.getStatefulViewFactory(),
                currentNavRoute.getRouteName(),
                overrideArgs,
                currentNavRoute.getNavPopCallback(),
                currentNavRoute.getRouteOptions());
        if (oldView != null) {
            NavViewLayout existingViewAnimator = getViewAnimator();
            if (newRoute.getStatefulView() instanceof StatefulViewDialog) {
                // the dialog branch of doPush does not swap the displayed
                // child: show the view of the route below the rebuilt one,
                // same as what the disposed route leaves behind. When the
                // rebuilt route was the initial one there is no route below,
                // the old (already disposed) view stays attached like the
                // previous implementation did.
                int underViewIndex = existingViewAnimator.indexInStack(oldView) - 1;
                if (underViewIndex >= 0) {
                    existingViewAnimator.animateTo(underViewIndex);
                }
            }
            // remove the rebuilt view's previous instance from the view stack
            // before draining pending routes and saving the state, the layout
            // child itself was already swapped by showNext() inside doPush for
            // a non-dialog destination, or by animateTo() above for a dialog
            // destination
            existingViewAnimator.removeFromStack(oldView);
        }
        finishNavigation(underRoute, newRoute);
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
        cancelOngoingPredictiveBack();
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
            NavViewLayout viewAnimator = getViewAnimator();
            int selectedIndex = calculateRouteIndexForViewAnimator(routeIndex);
            View childView = viewAnimator.getChildFromStack(selectedIndex);
            View currentView = viewAnimator.getCurrentView();
            View buildView = navRoute.getStatefulView().buildView(getActivity(), viewAnimator);
            viewAnimator.replaceStack(buildView, selectedIndex);
            if (childView == currentView) {
                // animate only when current view is showing
                viewAnimator.setInAnimation(mNavConfiguration.getDefaultReBuildEnterAnimation());
                viewAnimator.setOutAnimation(mNavConfiguration.getDefaultReBuildExitAnimation());
                viewAnimator.animateTo(selectedIndex);
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
        cancelOngoingPredictiveBack();
        mIsNavigating = true;
        NavViewLayout newViewAnimator = createViewAnimator(mActivity, mViewAnimatorId, mNavConfiguration);
        for (int i = mNavRouteStack.size() - 1; i >= 0; i--) {
            NavRoute navRoute = mNavRouteStack.get(i);
            StatefulView statefulView = navRoute.getStatefulView();
            if (statefulView instanceof StatefulViewDialog) {
                ((StatefulViewDialog) statefulView).initDialog(getActivity());
            } else {
                View view = statefulView.buildView(mActivity, newViewAnimator);
                newViewAnimator.addToStack(view);
            }
        }
        newViewAnimator.displayLast();
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
                    result = getViewAnimator().getChildFromStack(
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
        if (mPredictiveBackActive) {
            // this invocation is the COMMIT of a predictive back gesture,
            // consume the flag first so any re-entrant navigation triggered by
            // the pop below treats the gesture as already finished
            mPredictiveBackActive = false;
            NavViewLayout viewAnimator = getViewAnimator();
            if (viewAnimator != null && viewAnimator.isPredictiveBackActive()) {
                // Speculative-pop case: the gesture animation is running in the
                // layout, run the pop bookkeeping with the standard pop
                // transition suppressed, finishPredictiveBack() plays the
                // gesture-driven commit animation instead (the current page
                // continues sliding away revealing the previous page that is
                // already attached beneath).
                popInternal(null, true, true, true);
                return;
            }
            // Interceptor case: the top route implements NavOnBackPressed, its
            // back behavior is unknown (it may pop, it may not), so no gesture
            // animation was ever started. Deliberate correctness-over-coverage
            // decision: it would be wrong to animate a pop that may not
            // happen. Fall through to the legacy handling below which forwards
            // the back press to the page; since nothing was animated, there is
            // nothing to cancel either.
        }
        if (!mNavRouteStack.isEmpty()) {
            StatefulView statefulView = mNavRouteStack.peek().getStatefulView();
            if (statefulView instanceof NavOnBackPressed) {
                NavViewLayout viewAnimator = getViewAnimator();
                ((NavOnBackPressed) statefulView).onBackPressed(viewAnimator.getCurrentView(),
                        mActivity, this);
                return;
            }
        }
        // if back button is not handled, then pop as usual
        pop();
    }

    /**
     * Decides whether the NEXT back event will be consumed by this navigator.
     * Host apps use it to enable/disable their back dispatcher callback so the
     * system can show its default back-to-home animation when this returns
     * false.
     * <p>
     * The rules are:
     * <ul>
     *     <li>empty route stack (e.g. during the async snapshot-restore
     *     window) -> false, the system default (activity exit) applies</li>
     *     <li>top route is a StatefulViewDialog -> false, the dialog owns its
     *     window and consumes back itself</li>
     *     <li>stack size more than 1 -> true, the navigator can pop</li>
     *     <li>stack size == 1 -> true only if the top StatefulView implements
     *     NavOnBackPressed (e.g. a home page that closes a drawer on back),
     *     otherwise false because back would exit the activity anyway</li>
     * </ul>
     */
    @Override
    public boolean shouldInterceptBack() {
        if (mNavRouteStack.isEmpty()) {
            return false;
        }
        StatefulView currentStatefulView = mNavRouteStack.peek().getStatefulView();
        if (currentStatefulView instanceof StatefulViewDialog) {
            return false;
        }
        if (mNavRouteStack.size() > 1) {
            return true;
        }
        return currentStatefulView instanceof NavOnBackPressed;
    }

    @Override
    public void onBackStarted(NavBackEvent backEvent) {
        if (backEvent == null || mIsNavigating || !shouldInterceptBack()) {
            // defensive no-op: no gesture can be meaningfully tracked while a
            // navigation is running, when the stack is empty/being restored or
            // when back would not be consumed by this navigator anyway
            return;
        }
        StatefulView currentStatefulView = mNavRouteStack.peek().getStatefulView();
        int stackSize = mNavRouteStack.size();
        if (stackSize > 1 && !(currentStatefulView instanceof NavOnBackPressed)) {
            // Speculative pop: the top route has no custom back handling, so
            // back WILL pop it. Start the gesture-driven pop animation that
            // reveals the previous page underneath. The route itself is only
            // popped when the gesture commits via onBackPressed().
            NavViewLayout viewAnimator = getViewAnimator();
            if (viewAnimator != null && viewAnimator.startPredictiveBack(backEvent)) {
                mPredictiveBackActive = true;
            }
        } else if (stackSize > 1) {
            // The top route implements NavOnBackPressed: its back behavior is
            // unknown (pop, drawer close, ...). No animation is started on
            // purpose, correctness over coverage: it would be wrong to animate
            // a pop that may not happen. The flag is still set so that the
            // commit is routed through the gesture-aware onBackPressed() path,
            // which simply falls back to the legacy handling.
            mPredictiveBackActive = true;
        }
        // stackSize == 1 (e.g. a home page with drawer/double-back handling):
        // no animation and no flag, the commit simply goes through the legacy
        // onBackPressed() path
    }

    @Override
    public void onBackProgressed(NavBackEvent backEvent) {
        if (backEvent == null) {
            return;
        }
        if (!mPredictiveBackActive) {
            // defensive normalization: some hosts may deliver progressed
            // without a preceding started event (missed callback), behave like
            // onBackStarted first
            onBackStarted(backEvent);
        }
        if (mPredictiveBackActive) {
            NavViewLayout viewAnimator = getViewAnimator();
            if (viewAnimator != null && viewAnimator.isPredictiveBackActive()) {
                viewAnimator.updatePredictiveBack(backEvent.getProgress());
            }
            // when no animation is running (interceptor case) there is nothing
            // to update
        }
    }

    @Override
    public void onBackCancelled() {
        if (mPredictiveBackActive) {
            mPredictiveBackActive = false;
            NavViewLayout viewAnimator = getViewAnimator();
            if (viewAnimator != null) {
                viewAnimator.cancelPredictiveBack();
            }
        }
    }

    /**
     * Cancels any in-flight predictive back gesture. Invoked at the entry of
     * every mutating navigation method (push, popInternal, replace, retry,
     * reBuildRoute, reBuildAllRoute) and dispose(), so the layout never
     * enters a navigation while the two-children-during-gesture window is
     * open. Cheap no-op when no gesture is active.
     */
    private void cancelOngoingPredictiveBack() {
        if (mPredictiveBackActive) {
            mPredictiveBackActive = false;
            NavViewLayout viewAnimator = getViewAnimator();
            if (viewAnimator != null) {
                viewAnimator.cancelPredictiveBack();
            }
        }
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

    private NavViewLayout createViewAnimator(ACT activity, int viewAnimatorId, NavConfiguration navConfiguration) {
        NavViewLayout viewAnimator = new NavViewLayout(activity, this);
        viewAnimator.setId(viewAnimatorId);
        viewAnimator.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return viewAnimator;
    }

    protected void setViewAnimator(ACT activity, NavViewLayout viewAnimator) {
        activity.setContentView(viewAnimator);
    }

    protected void initNavigatorState(NavViewLayout viewAnimator) {
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
                            viewAnimator.addToStack(statefulView
                                    .buildView(mActivity, viewAnimator)
                            );
                        }
                    }
                    viewAnimator.displayLast();
                    checkAndShowDialog();
                    checkAndConfigureRequestOrientation();
                    initViewNavigator();
                    mIsNavigating = false;
                    if (!mPendingNavigatorRoute.isEmpty()) {
                        mPendingNavigatorRoute.pop().run();
                    }
                    // Announce the restore to route-changed listeners. This
                    // block only runs when a snapshot was actually restored
                    // (a first launch goes through the else-branch push above,
                    // which fires the event through normal navigation), so
                    // there is no double fire. It is deliberately the LAST
                    // statement: listeners observe fully-consistent state
                    // (views built, displayed, dialogs shown, orientation
                    // applied) and may navigate safely from here - any
                    // navigation they trigger queues/drains through the
                    // normal pending-route mechanism. Without this event,
                    // hosts syncing their back dispatcher callback enabled
                    // state via addOnRouteChangedListener go stale after
                    // process death (the callback was synced against the
                    // empty stack during onCreate, before this async restore
                    // replaced it).
                    onRouteChanged(null, mNavRouteStack.peek());
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
        NavViewLayout viewAnimator = createViewAnimator(mActivity, mViewAnimatorId,
                mNavConfiguration);
        setViewAnimator(mActivity, viewAnimator);
        initNavigatorState(viewAnimator);
    }

    private void checkAndConfigureRequestOrientation() {
        Activity activity = getActivity();
        int activityOrientation = activity.getRequestedOrientation();
        NavRoute navRoute = mNavRouteStack.peek();
        if (navRoute == null) {
            if (activityOrientation != mActivityDefaultScreenOrientation) {
                activity.setRequestedOrientation(mActivityDefaultScreenOrientation);
            }
            return;
        }
        StatefulView currentRoute = navRoute.getStatefulView();
        if (currentRoute instanceof NavActivity.RequestOrientation) {
            int routeOrientation = ((NavActivity.RequestOrientation) currentRoute).getRequestedOrientation();
            if (activityOrientation != routeOrientation) {
                activity.setRequestedOrientation(routeOrientation);
            }
        } else {
            if (activityOrientation != mActivityDefaultScreenOrientation) {
                activity.setRequestedOrientation(mActivityDefaultScreenOrientation);
            }
        }
    }

    @Override
    public ACT getActivity() {
        return mActivity;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        if (mActivityClass.isInstance(activity)) {
            mActivity = (ACT) activity;
            mActivityDefaultScreenOrientation = activity.getRequestedOrientation();
            String packageName = activity.getPackageName();
            Integer versionCode = null;
            String versionName = null;
            File saveStateFile = null;
            try {
                PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(packageName, 0);
                versionCode = packageInfo.versionCode;
                versionName = packageInfo.versionName;
                saveStateFile = new File(activity.getCacheDir(), "m.co.rh.id.anavigator/"
                        + packageName + "_" + versionName + "_" + versionCode +
                        "/" + mActivity.getClass().getName());
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Package info not found for: " + packageName, e);
            }
            if (mNavConfiguration.getSaveStateFile() == null && saveStateFile != null) {
                mNavConfiguration.setSaveStateFile(saveStateFile);
            }
            if (!mViewNavigatorList.isEmpty() && saveStateFile != null) {
                for (ViewNavigator viewNavigator : mViewNavigatorList) {
                    NavConfiguration navConfiguration = viewNavigator.getNavConfiguration();
                    if (navConfiguration.getSaveStateFile() == null) {
                        Integer containerId = viewNavigator.getViewGroupContainerId();
                        File viewNavSaveStateFile = new File(activity.getCacheDir(), "m.co.rh.id.anavigator/"
                                + packageName + "_" + versionName + "_" + versionCode +
                                "/" + mActivity.getClass().getName() + "_" + "viewNavigator_" + containerId);
                        navConfiguration.setSaveStateFile(viewNavSaveStateFile);
                    }
                }
            }
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
        if (mActivityClass.isInstance(activity)) {
            if (!mNavRouteStack.isEmpty()) {
                for (NavRoute navRoute : mNavRouteStack) {
                    StatefulView statefulView = navRoute.getStatefulView();
                    if (statefulView instanceof NavActivityLifecycle) {
                        Runnable onActivityStarted = () -> ((NavActivityLifecycle) statefulView).onNavActivityStarted(mActivity);
                        if (statefulView instanceof StatefulViewDialog) {
                            mHandler.post(onActivityStarted);
                        } else {
                            onActivityStarted.run();
                        }
                    }
                }
            }
            // handle view navigator
            if (!mViewNavigatorList.isEmpty()) {
                for (ViewNavigator viewNavigator : mViewNavigatorList) {
                    viewNavigator.onActivityStarted(activity);
                }
            }
        }
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
        if (mActivityClass.isInstance(activity)) {
            if (!mNavRouteStack.isEmpty()) {
                for (NavRoute navRoute : mNavRouteStack) {
                    StatefulView statefulView = navRoute.getStatefulView();
                    if (statefulView instanceof NavActivityLifecycle) {
                        Runnable onActivityStopped = () -> ((NavActivityLifecycle) statefulView).onNavActivityStopped(mActivity);
                        if (statefulView instanceof StatefulViewDialog) {
                            mHandler.post(onActivityStopped);
                        } else {
                            onActivityStopped.run();
                        }
                    }
                }
            }
            // handle view navigator
            if (!mViewNavigatorList.isEmpty()) {
                for (ViewNavigator viewNavigator : mViewNavigatorList) {
                    viewNavigator.onActivityStopped(activity);
                }
            }
        }
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
        NavViewLayout viewAnimator = getViewAnimator();
        if (viewAnimator != null) {
            viewAnimator.clearCache();
        }
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
            // Defensive copy taken on the caller (main) thread: the background
            // task below serializes the object while the main thread may keep
            // mutating the live route stack (e.g. draining a pending
            // navigation), which would throw ConcurrentModificationException,
            // get swallowed and silently skip the save.
            final Serializable stateToSave = serializable instanceof LinkedList
                    ? new LinkedList<>((LinkedList) serializable)
                    : serializable;
            mStateSnapshot = getExecutorService().submit(() -> {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    oos.writeObject(new SealedObject(stateToSave, encryptCipher));
                    oos.close();
                    bos.close();
                    fileOutputStream.close();
                } catch (Throwable throwable) {
                    Log.e(TAG, "Failed to save state", throwable);
                }
                return stateToSave;
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
 * Extend Scene because Scene doesn't have proper hashCode and equals calculation.
 * Scene keys are cached in TransitionManager which uses ArrayMap internally.
 * This ArrayMap has the same behavior as HashMap which depends on hashCode to set and remove value.
 * If TransitionManager.setTransition is being called multiple times to the same SceneRoot and Layout instances,
 * then it is possible for out of memory to happen since the internal ArrayMap keeps growing and old values never replaced.
 */
class NavScene extends Scene {

    private View mLayout;

    public NavScene(ViewGroup sceneRoot, View layout) {
        super(sceneRoot, layout);
        mLayout = layout;
    }

    @Override
    public int hashCode() {
        return getSceneRoot().hashCode() + mLayout.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NavScene)) return false;

        NavScene navScene = (NavScene) o;

        return getSceneRoot().equals(navScene.getSceneRoot()) && mLayout.equals(navScene.mLayout);
    }
}

class NavViewLayout extends FrameLayout {

    private Animation mInAnimation;
    private Animation mOutAnimation;
    private final Navigator mNavigator;
    private final LinkedList<View> mViewStack;
    private Map<Integer, Animation> mAnimationCache;
    private Map<Integer, Transition> mTransitionCache;

    // -----------------------------------------------------------------------------------------
    // Predictive back gesture state.
    // During the gesture the view of the route BELOW the current one is attached
    // at index 0 (bottom Z order) while the current view stays topmost (it is
    // the last child) and is transformed following the gesture progress. The
    // historical single-child invariant is restored by
    // cancelPredictiveBack()/finishPredictiveBack().
    // -----------------------------------------------------------------------------------------
    /**
     * -1 when the gesture started on the left half of the screen, +1 on the
     * right half: the current page slides toward the gesture edge.
     */
    private float mPredictiveBackDirection;
    private View mPredictiveBackPreviousView;
    private boolean mPredictiveBackActive;

    public NavViewLayout(Context context, Navigator navigator) {
        super(context);
        mNavigator = navigator;
        mViewStack = new LinkedList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mAnimationCache = new ArrayMap<>();
            mTransitionCache = new ArrayMap<>();
        } else {
            mAnimationCache = new HashMap<>();
            mTransitionCache = new HashMap<>();
        }
    }

    public void clearCache() {
        mAnimationCache.clear();
        mTransitionCache.clear();
    }

    private Transition getTransitionFromCache(Integer transitionId) {
        Transition transition = mTransitionCache.get(transitionId);
        if (transition == null) {
            transition = TransitionInflater.from(getContext()).inflateTransition(transitionId);
            mTransitionCache.put(transitionId, transition);
        }
        return transition;
    }

    private Animation getAnimationFromCache(Integer animationId) {
        Animation animation = mAnimationCache.get(animationId);
        if (animation == null) {
            animation = AnimationUtils.loadAnimation(getContext(), animationId);
            mAnimationCache.put(animationId, animation);
        }
        return animation;
    }

    public void addToStack(View view) {
        mViewStack.add(view);
    }

    public void removeFromStack(View view) {
        mViewStack.remove(view);
    }

    /**
     * @return the index of the given view inside the view stack, -1 if not found
     */
    public int indexInStack(View view) {
        return mViewStack.indexOf(view);
    }

    public void popStack() {
        View poppedView = mViewStack.pop();
        removeView(poppedView);
    }

    public void replaceStack(View view, int index) {
        mViewStack.set(index, view);
    }

    public void displayLast() {
        sweepDetachedViews();
        removeAllViews();
        // guard: an empty view stack (e.g. a route stack consisting of dialogs
        // only) has nothing to display, this used to throw NoSuchElement
        if (!mViewStack.isEmpty()) {
            addView(mViewStack.getLast());
        }
    }

    public void setInAnimation(Animation inAnimation) {
        this.mInAnimation = inAnimation;
    }

    public void setOutAnimation(Animation outAnimation) {
        this.mOutAnimation = outAnimation;
    }

    public void animateTo(int index) {
        // self-heal for the interrupted predictive back commit: the animating
        // out view is no longer part of the view stack, remove it before any
        // index arithmetic or child swap happens
        sweepDetachedViews();
        if (index < 0 || index >= mViewStack.size()) {
            // defensive clamp: never crash on IndexOutOfBoundsException and
            // shield getCurrentRoute() from NPE on an empty route stack below
            Log.e(NavViewLayout.class.getSimpleName(),
                    "animateTo(" + index + ") out of bounds, mViewStack size="
                            + mViewStack.size());
            return;
        }
        View currentView = getCurrentView();
        View destView = mViewStack.get(index);
        int currentViewIdx = mViewStack.indexOf(currentView);
        RouteOptions currentRouteOptions = mNavigator.getCurrentRoute().getRouteOptions();
        Transition destTransition = null;
        if (currentViewIdx > index) {
            // show prev view
            if (currentRouteOptions instanceof TransitionRouteOptions) {
                Integer exitTransitionResId = ((TransitionRouteOptions) currentRouteOptions).exitTransitionResId;
                if (exitTransitionResId != null) {
                    destTransition = getTransitionFromCache(exitTransitionResId);
                }
            }
        } else if (currentViewIdx < index) {
            // show next view
            if (currentRouteOptions instanceof TransitionRouteOptions) {
                Integer enterTransitionResId = ((TransitionRouteOptions) currentRouteOptions).enterTransitionResId;
                if (enterTransitionResId != null) {
                    destTransition = getTransitionFromCache(enterTransitionResId);
                }
            }
        }
        if (destTransition != null) {
            // force-complete any pending or running transition on this scene
            // root first: androidx.transition's TransitionManager.changeScene
            // silently skips the whole scene swap when
            // sPendingTransitions.contains(sceneRoot), which would swallow a
            // same-frame consecutive navigation and leave the previous view
            // displayed. The interrupted animation on rapid navigation is an
            // acceptable visual cost.
            TransitionManager.endTransitions(this);
            Scene nextScene = new NavScene(this, destView);
            // Use the static go() instead of TransitionManager.setTransition
            // plus transitionTo(): setTransition accumulates a permanent
            // Scene -> Transition map entry per navigation and every NavScene
            // strongly references its (soon discarded) route view, which leaks
            // memory across a session. transitionTo(scene) only resolves the
            // transition through that map right before invoking
            // changeScene(scene, transition), so passing the transition
            // directly to go() is behaviorally identical while the map is
            // never populated.
            TransitionManager.go(nextScene, destTransition);
        } else {
            // no transition, use default
            if (currentView != null) {
                if (mOutAnimation != null) {
                    currentView.startAnimation(mOutAnimation);
                }
                removeView(currentView);
            }
            if (mInAnimation != null) {
                destView.startAnimation(mInAnimation);
            }
            if (destView.getParent() == this) {
                // can happen when a predictive back cancel ran in the same
                // frame: the revealed view is still attached (its removal is
                // deferred to the cancel animation end action), detach it so
                // the swap below stays valid and it ends up on top
                removeView(destView);
            }
            addView(destView);
        }
    }

    /**
     * @return the topmost attached child that is still part of the view stack.
     * With the historical single-child invariant this is the only attached
     * child and equals the old {@code getChildAt(0)}. During a predictive back
     * gesture (previous attached at index 0, current on top at the last index)
     * it returns the true current view, and during the brief predictive back
     * commit animation window it skips the animating-out view which is already
     * removed from the view stack but still attached to play its exit
     * animation. Falls back to the plain topmost child when no attached child
     * is part of the view stack (e.g. the loading view shown before the first
     * route is restored), preserving the legacy behavior in that case.
     */
    public View getCurrentView() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (mViewStack.contains(child)) {
                return child;
            }
        }
        if (getChildCount() == 0) {
            return null;
        }
        return getChildAt(getChildCount() - 1);
    }

    public View getChildFromStack(int index) {
        return mViewStack.get(index);
    }

    private int getCurrentViewIndex() {
        View currentView = getCurrentView();
        return mViewStack.indexOf(currentView);
    }

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
                        inAnimation = getAnimationFromCache(inAnimationId);
                    } catch (Throwable t) {
                        inAnimation = null;
                    }
                } else {
                    inAnimation = null;
                }
                Integer outAnimationId = routeOptions.getPopExitAnimationResId();
                if (outAnimationId != null) {
                    try {
                        outAnimation = getAnimationFromCache(outAnimationId);
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
        animateTo(getCurrentViewIndex() - 1);
    }

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
                        inAnimation = getAnimationFromCache(inAnimationId);
                    } catch (Throwable t) {
                        inAnimation = null;
                    }
                } else {
                    inAnimation = null;
                }
                Integer outAnimationId = routeOptions.getExitAnimationResId();
                if (outAnimationId != null) {
                    try {
                        outAnimation = getAnimationFromCache(outAnimationId);
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
        animateTo(getCurrentViewIndex() + 1);
    }

    // -----------------------------------------------------------------------------------------
    // Predictive back gesture mechanics.
    // All view APIs used here are available since API 11 (setTranslationX,
    // setScaleX/Y, setAlpha); ViewPropertyAnimator (View.animate()) itself is
    // available since API 12, but withEndAction - used here for the deferred
    // child removals - requires API 16, hence the JELLY_BEAN guard that falls
    // back to instant transforms on the minSdk 14/15 devices.
    // -----------------------------------------------------------------------------------------

    /**
     * @return true while a predictive back gesture animation is in progress,
     * meaning the previous page view is attached beneath the current one
     */
    public boolean isPredictiveBackActive() {
        return mPredictiveBackActive;
    }

    /**
     * Starts the predictive back gesture: attaches the view of the route below
     * the current one at index 0 (bottom Z order) with an initial 0.95 scale,
     * the current view stays topmost untouched.
     *
     * @param event the gesture start event, its touch point decides the slide
     *              direction (toward the gesture edge)
     * @return true if the gesture animation was started, false when there is
     * no previous view to reveal (nothing to animate)
     */
    public boolean startPredictiveBack(NavBackEvent event) {
        // self-heal: an interrupted earlier gesture (its end action never ran,
        // e.g. the layout was detached mid-animation) must not leak its extra
        // child into the new gesture
        cleanupStalePredictiveBack();
        View currentView = getCurrentView();
        int currentIndex = currentView == null ? -1 : mViewStack.indexOf(currentView);
        if (currentIndex <= 0) {
            // nothing beneath the current view, nothing to reveal
            return false;
        }
        mPredictiveBackDirection = resolvePredictiveBackDirection(event);
        mPredictiveBackPreviousView = mViewStack.get(currentIndex - 1);
        // cancel a stale restore animator still running on the current view
        // from a previous gesture cancellation: a fast cancel->re-swipe
        // (under the ~180ms restore duration) would otherwise let the stale
        // animator fight updatePredictiveBack on every animation frame and
        // snap the view back to identity when it ends. Canceling a
        // ViewPropertyAnimator does NOT run its withEndAction, so the
        // deferred previous-view removal of the old cancel cannot fire here;
        // on API < 16 there is no ViewPropertyAnimator at all, hence nothing
        // to cancel. Note: if the stale end action HAD already detached the
        // previous view, the attach block below re-adds it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            currentView.animate().cancel();
        }
        // attach the previous view at the bottom of the Z order, the current
        // view (topmost, last child) stays on top of it. It may still be
        // attached from a cancelled gesture whose restore animation has not
        // completed yet: in that case reuse it in place (its deferred removal
        // end action becomes a no-op through the mPredictiveBackActive check)
        View previousView = mPredictiveBackPreviousView;
        if (previousView.getParent() == this) {
            if (indexOfChild(previousView) != 0) {
                removeView(previousView);
                addView(previousView, 0);
            }
        } else {
            addView(previousView, 0);
        }
        previousView.setScaleX(0.95f);
        previousView.setScaleY(0.95f);
        mPredictiveBackActive = true;
        return true;
    }

    /**
     * Updates the ongoing predictive back gesture with a new progress value.
     * The current view scales down (1.0 -> 0.9) and slides toward the gesture
     * edge while the previous view scales up (0.95 -> 1.0) proportionally.
     *
     * @param progress gesture progress, clamped to 0..1
     */
    public void updatePredictiveBack(float progress) {
        if (!mPredictiveBackActive) {
            return;
        }
        float clamped = Math.max(0f, Math.min(1f, progress));
        View currentView = getCurrentView();
        if (currentView != null && currentView != mPredictiveBackPreviousView) {
            currentView.setScaleX(1f - 0.1f * clamped);
            currentView.setScaleY(1f - 0.1f * clamped);
            currentView.setTranslationX(mPredictiveBackDirection * 0.15f * getWidth() * clamped);
        }
        if (mPredictiveBackPreviousView != null) {
            float scale = 0.95f + 0.05f * clamped;
            mPredictiveBackPreviousView.setScaleX(scale);
            mPredictiveBackPreviousView.setScaleY(scale);
        }
    }

    /**
     * Cancels the ongoing predictive back gesture: animates the current view
     * back to its identity transform and removes the previous view again once
     * the animation ends, restoring the single-child invariant. No-op when no
     * gesture is active.
     */
    public void cancelPredictiveBack() {
        if (!mPredictiveBackActive) {
            return;
        }
        mPredictiveBackActive = false;
        View previousView = mPredictiveBackPreviousView;
        View currentView = getCurrentView();
        mPredictiveBackPreviousView = null;
        if (currentView == null || previousView == null || currentView == previousView) {
            // inconsistent/already-repaired state, restore the transforms
            // defensively and drop the extra child if it is still attached
            resetPredictiveBackTransforms(currentView);
            resetPredictiveBackTransforms(previousView);
            if (previousView != null && previousView.getParent() == this) {
                removeView(previousView);
            }
            return;
        }
        // animate the current view back to identity, then detach the previous
        // view. The end action is defensive: it removes only the captured
        // previous view, and only while no NEW gesture re-anchored it
        // (mPredictiveBackActive) and while it did not become the legitimately
        // displayed view (a navigation may have swapped to it in the same
        // frame as this cancellation)
        runPredictiveBackAnimation(currentView, 180L, 0f, 1f, 1f, 1f, false, () -> {
            if (!mPredictiveBackActive
                    && previousView.getParent() == this
                    && previousView != getCurrentView()) {
                previousView.setScaleX(1f);
                previousView.setScaleY(1f);
                removeView(previousView);
            }
        });
    }

    /**
     * Finishes the predictive back gesture as a COMMIT: plays the pop
     * animation where the current page continues sliding away (translate,
     * shrink, fade) revealing the previous page that is already attached
     * beneath it, then detaches the animating-out view. The route bookkeeping
     * runs synchronously in the navigator in parallel; the end action only
     * removes the captured animating-out view so a navigation issued during
     * the ~200ms animation window cannot break the hierarchy (stale children
     * are additionally swept by {@link #sweepDetachedViews()}). No-op when no
     * gesture is active.
     */
    public void finishPredictiveBack() {
        if (!mPredictiveBackActive) {
            return;
        }
        mPredictiveBackActive = false;
        View previousView = mPredictiveBackPreviousView;
        View currentView = getCurrentView();
        mPredictiveBackPreviousView = null;
        float direction = mPredictiveBackDirection;
        if (currentView == null || previousView == null || currentView == previousView) {
            // inconsistent/already-repaired state, restore transforms
            // defensively, the caller's bookkeeping takes care of the rest
            resetPredictiveBackTransforms(currentView);
            resetPredictiveBackTransforms(previousView);
            if (previousView != null && previousView.getParent() == this && previousView != currentView) {
                removeView(previousView);
            }
            return;
        }
        float width = getWidth();
        // bring the revealed page to full scale while the current page slides
        // out toward the gesture edge, shrinks and fades away
        runPredictiveBackAnimation(previousView, 200L, 0f, 1f, 1f, 1f, false, null);
        runPredictiveBackAnimation(currentView, 200L,
                direction * 0.5f * width, 0.85f, 0.85f, 0f, true, () -> {
                    // remove only the captured view: the route bookkeeping
                    // already ran, so the layout may have been navigated again
                    // in the meantime and this view may even have been swept
                    if (currentView.getParent() == this) {
                        removeView(currentView);
                    }
                    currentView.setAlpha(1f);
                });
    }

    /**
     * Removes any attached child that is NOT part of the view stack.
     * Self-heal for the predictive back commit window: the animating-out view
     * stays attached for its ~200ms exit animation after the route was already
     * popped (and removed from the view stack), so a navigation landing inside
     * that window must be able to get rid of it deterministically instead of
     * waiting for the animation end action.
     */
    public void sweepDetachedViews() {
        int childCount = getChildCount();
        if (childCount > 1) {
            // iterate backwards, removeView shifts the indices
            for (int i = childCount - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (!mViewStack.contains(child)) {
                    removeView(child);
                }
            }
        }
    }

    private float resolvePredictiveBackDirection(NavBackEvent event) {
        // slide toward the screen edge the gesture started from
        return event.getTouchX() < getWidth() / 2f ? -1f : 1f;
    }

    /**
     * Repairs leftover gesture state from an interrupted gesture whose end
     * action never ran (e.g. the layout was detached mid-animation): resets
     * the previous view transforms and detaches it.
     */
    private void cleanupStalePredictiveBack() {
        if (mPredictiveBackPreviousView != null) {
            View previousView = mPredictiveBackPreviousView;
            previousView.setScaleX(1f);
            previousView.setScaleY(1f);
            if (previousView.getParent() == this) {
                removeView(previousView);
            }
            mPredictiveBackPreviousView = null;
        }
        mPredictiveBackActive = false;
    }

    private void resetPredictiveBackTransforms(View view) {
        if (view != null) {
            view.setTranslationX(0f);
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.setAlpha(1f);
        }
    }

    /**
     * Animates a single view to the given target transform values.
     * Uses ViewPropertyAnimator on API 16+; below that it falls back to
     * applying the target values instantly and running the end action
     * immediately (the gesture still works, just without the tween).
     *
     * @param applyAlpha false to leave the view alpha untouched
     */
    private void runPredictiveBackAnimation(View view, long durationMs,
                                            float targetTranslationX,
                                            float targetScaleX, float targetScaleY,
                                            float targetAlpha, boolean applyAlpha,
                                            Runnable endAction) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ViewPropertyAnimator animator = view.animate()
                    .translationX(targetTranslationX)
                    .scaleX(targetScaleX)
                    .scaleY(targetScaleY)
                    .setDuration(durationMs);
            if (applyAlpha) {
                animator.alpha(targetAlpha);
            }
            if (endAction != null) {
                animator.withEndAction(endAction);
            }
        } else {
            view.setTranslationX(targetTranslationX);
            view.setScaleX(targetScaleX);
            view.setScaleY(targetScaleY);
            if (applyAlpha) {
                view.setAlpha(targetAlpha);
            }
            if (endAction != null) {
                endAction.run();
            }
        }
    }
}