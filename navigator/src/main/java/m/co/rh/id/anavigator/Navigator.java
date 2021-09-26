package m.co.rh.id.anavigator;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
@SuppressWarnings({"unchecked", "rawtypes"})
public class Navigator<ACT extends Activity, SV extends StatefulView> implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2, INavigator {
    private static final String TAG = "Navigator";
    /**
     * Key used to retrieve value from Intent data when navigator {@link #pop(Serializable)} and finishes the activity.
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
    private SnapshotHandler mNavSnapshotHandler;

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
        mNavSnapshotHandler = new SnapshotHandler(navConfiguration,
                "m.co.rh.id.anavigator.Navigator.NavSharedPreference-" +
                        mActivityClass.getName(),
                "m.co.rh.id.anavigator.Navigator.NavSharedPreference.StateKey-" +
                        mActivityClass.getName());
    }

    @Override
    public void push(String routeName, Serializable args, NavPopCallback navPopCallback) {
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
        ViewAnimator existingViewAnimator = mActivity.findViewById(mContainerId);
        View view = statefulView.buildView(mActivity, existingViewAnimator);
        existingViewAnimator.addView(view);
        existingViewAnimator.showNext();
        mIsNavigating = false;
        if (!mPendingNavigatorRoute.isEmpty()) {
            mPendingNavigatorRoute.pop().run();
        }
        mNavSnapshotHandler.saveState(mActivity, mNavRouteStack);
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
            ViewAnimator existingViewAnimator = mActivity.findViewById(mContainerId);
            View currentView = existingViewAnimator.getCurrentView();
            existingViewAnimator.showPrevious();
            popStack(existingViewAnimator.getCurrentView(), result);
            existingViewAnimator.removeView(currentView);
            mIsNavigating = false;
            if (!mPendingNavigatorRoute.isEmpty()) {
                mPendingNavigatorRoute.pop().run();
            }
            mNavSnapshotHandler.saveState(mActivity, mNavRouteStack);
            return true;
        } else {
            mPendingNavigatorRoute.clear();
            // pop initial route
            if (mNavRouteStack.size() == 1) {
                popStack(null, result);
            }
            int activityResult = Activity.RESULT_CANCELED;
            if (result != null) {
                activityResult = Activity.RESULT_OK;
            }
            setActivityResultAndFinish(activityResult, result);
            mIsNavigating = false;
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
            Serializable routeStack = mNavSnapshotHandler.loadState(activity);
            if (routeStack != null) {
                mNavRouteStack = (LinkedList<NavRoute>) routeStack;
                // re-inject navigator
                for (NavRoute navRoute : mNavRouteStack) {
                    StatefulView statefulView = navRoute.getStatefulView();
                    if (statefulView instanceof RequireNavigator) {
                        ((RequireNavigator) statefulView).provideNavigator(this);
                    }
                }
                Log.i(TAG, "restored navigator state");
            }
            if (!mNavRouteStack.isEmpty()) {
                // re-add all the views
                for (NavRoute navRoute : mNavRouteStack) {
                    viewAnimator.addView(navRoute
                            .getStatefulView()
                            .buildView(mActivity, viewAnimator));
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
        if (mActivityClass.isInstance(activity)) {
            mNavSnapshotHandler.saveState(activity, mNavRouteStack);
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (mActivityClass.isInstance(activity)) {
            mActivity = null;
            if (activity.isFinishing()) {
                mNavSnapshotHandler.clearState(activity);
                mNavSnapshotHandler.dispose();
            }
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

// save state to SharedPreferences.
// during Activity.onCreate(bundle), the bundle value seemed to always null,
// and it haven't been fix until 2020, see https://issuetracker.google.com/issues/37020082
@SuppressWarnings("rawtypes")
class SnapshotHandler {
    private ExecutorService executorService;
    private Future<Serializable> stateSnapshot;
    private NavConfiguration navConfiguration;
    private String sharedPrefName;
    private String sharedPrefStateKey;

    SnapshotHandler(NavConfiguration navConfiguration, String sharedPrefName, String sharedPrefStateKey) {
        this.navConfiguration = navConfiguration;
        this.sharedPrefName = sharedPrefName;
        this.sharedPrefStateKey = sharedPrefStateKey;
    }

    void saveState(Activity activity, Serializable serializable) {
        if (navConfiguration.isSaveStateToSharedPreference()) {
            stateSnapshot = getExecutorService().submit(() -> {
                String snapshot = serializeToString(serializable);
                if (snapshot == null) {
                    throw new IllegalStateException("unable to save state, snapshot cant be serialized");
                }
                SharedPreferences sharedPreferences = activity.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
                sharedPreferences.edit().putString(sharedPrefStateKey, snapshot).commit();
                return snapshot;
            });
        }
    }

    Serializable loadState(Activity activity) {
        if (navConfiguration.isSaveStateToSharedPreference()) {
            if (stateSnapshot != null) {
                return getState();
            }
            stateSnapshot = getExecutorService().submit(() -> {
                SharedPreferences sharedPreferences = activity.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
                String serializedSnapshot = sharedPreferences.getString(sharedPrefStateKey, null);
                return deserializeToString(serializedSnapshot);
            });
            return getState();
        }
        return null;
    }

    void clearState(Activity activity) {
        if (stateSnapshot != null) {
            stateSnapshot.cancel(false);
            stateSnapshot = null;
        }
        getExecutorService().submit(() -> {
            SharedPreferences sharedPreferences = activity.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
            sharedPreferences.edit().clear().commit();
        });
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
        return executorService;
    }

    private String serializeToString(Serializable serializable) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(serializable);
        oos.close();
        baos.close();
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private Serializable deserializeToString(String serializableString) throws IOException, ClassNotFoundException {
        Serializable result = null;
        if (serializableString != null) {
            byte[] base64decodedBytes = Base64.decode(serializableString, Base64.DEFAULT);
            InputStream in = new ByteArrayInputStream(base64decodedBytes);
            ObjectInputStream oin = new ObjectInputStream(in);
            result = (Serializable) oin.readObject();
            oin.close();
            in.close();
        }
        return result;
    }

    private Serializable getState() {
        if (stateSnapshot != null) {
            try {
                // prevent ANR
                while (!stateSnapshot.isDone()) {
                    Thread.yield();
                }
                return stateSnapshot.get();
            } catch (Exception e) {
                Log.e("getState", "Unable to get snapshot", e);
            }
        }
        return null;
    }

    void dispose() {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
        if (stateSnapshot != null) {
            stateSnapshot.cancel(false);
            stateSnapshot = null;
        }
    }
}