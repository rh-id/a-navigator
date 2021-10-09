package m.co.rh.id.anavigator;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.NavActivityLifecycle;
import m.co.rh.id.anavigator.component.NavComponentCallback;
import m.co.rh.id.anavigator.component.NavOnActivityResult;
import m.co.rh.id.anavigator.component.NavOnBackPressed;
import m.co.rh.id.anavigator.component.NavOnRouteChangedListener;
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
    public void push(StatefulViewFactory statefulViewFactory, Serializable args, NavPopCallback navPopCallback) {
        if (mIsNavigating) {
            // It is possible that push is invoked somewhere during initState or buildView
            // put this invocation later after previous push is done
            mPendingNavigatorRoute.add(() -> push(statefulViewFactory, args, navPopCallback));
            return;
        }
        mIsNavigating = true;
        push(statefulViewFactory, null, args, navPopCallback);
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
        push(statefulViewFactory, routeName, args, navPopCallback);
    }

    private void push(StatefulViewFactory statefulViewFactory, String routeName, Serializable args, NavPopCallback navPopCallback) {
        SV statefulView = (SV) statefulViewFactory.newInstance(args, mActivity);
        NavRoute currentRoute = mNavRouteStack.peek();
        NavRoute newRoute = new NavRoute(statefulViewFactory, navPopCallback, statefulView, routeName, args, statefulView.getKey());
        // push must be done before initState or buildView so that the statefulView could get route information
        mNavRouteStack.push(newRoute);
        if (statefulView instanceof RequireNavigator) {
            ((RequireNavigator) statefulView).provideNavigator(this);
        }
        ViewAnimator existingViewAnimator = getViewAnimator();
        View view = statefulView.buildView(mActivity, existingViewAnimator);
        existingViewAnimator.addView(view);
        existingViewAnimator.showNext();
        // try to init view navigator everytime new route is pushed
        initViewNavigator();
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
            ViewAnimator existingViewAnimator = getViewAnimator();
            View currentView = existingViewAnimator.getCurrentView();
            existingViewAnimator.showPrevious();
            // Try reset view navigator before pop
            resetViewNavigator(currentView);
            // cont pop
            popStack(existingViewAnimator.getCurrentView(), result);
            existingViewAnimator.removeView(currentView);
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
        ViewAnimator existingViewAnimator = getViewAnimator();
        View currentView = existingViewAnimator.getCurrentView();
        NavRoute currentNavRoute = mNavRouteStack.pop();
        StatefulView statefulView = currentNavRoute.getStatefulView();
        if (statefulView != null) {
            statefulView.dispose(mActivity);
        }
        existingViewAnimator.removeView(currentView);
        push(currentNavRoute.getStatefulViewFactory(),
                currentNavRoute.getRouteName(),
                overrideArgs,
                currentNavRoute.getNavPopCallback());
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

    protected ViewAnimator createViewAnimator(ACT activity, int viewAnimatorId, NavConfiguration navConfiguration) {
        ViewAnimator viewAnimator = new ViewAnimator(activity);
        viewAnimator.setId(viewAnimatorId);
        viewAnimator.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        viewAnimator.setInAnimation(navConfiguration.getDefaultInAnimation());
        viewAnimator.setOutAnimation(navConfiguration.getDefaultOutAnimation());
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
            int last = mNavRouteStack.size() - 1;
            for (int i = last; i >= 0; i--) {
                NavRoute navRoute = mNavRouteStack.get(i);
                viewAnimator.addView(navRoute
                        .getStatefulView()
                        .buildView(mActivity, viewAnimator)
                );
            }
            viewAnimator.setDisplayedChild(last);
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
    private ExecutorService mExecutorService;
    private Future<Serializable> mStateSnapshot;
    private File mFile;

    SnapshotHandler(NavConfiguration navConfiguration) {
        mFile = navConfiguration.getSaveStateFile();
    }

    void saveState(Serializable serializable) {
        if (mFile != null) {
            mStateSnapshot = getExecutorService().submit(() -> {
                String snapshot = serializeToString(serializable);
                if (snapshot == null) {
                    throw new IllegalStateException("unable to save state, snapshot cant be serialized");
                }
                if (!mFile.exists()) {
                    mFile.createNewFile();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(mFile);
                fileOutputStream.write(snapshot.getBytes());
                fileOutputStream.close();
                return serializable;
            });
        }
    }

    Serializable loadState() {
        if (mFile != null) {
            if (mStateSnapshot != null) {
                return getState();
            }
            mStateSnapshot = getExecutorService().submit(() -> {
                if (!mFile.exists()) {
                    return null;
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                FileInputStream input = new FileInputStream(mFile);
                byte[] buffer = new byte[2048];
                int b;
                while (-1 != (b = input.read(buffer))) {
                    output.write(buffer, 0, b);
                }
                return deserialize(output.toString());
            });
            return getState();
        }
        return null;
    }

    void clearState() {
        if (mStateSnapshot != null) {
            mStateSnapshot.cancel(false);
            mStateSnapshot = null;
        }
        getExecutorService().submit(() -> {
            mFile.delete();
        });
    }

    private ExecutorService getExecutorService() {
        if (mExecutorService == null) {
            mExecutorService = Executors.newSingleThreadExecutor();
        }
        return mExecutorService;
    }

    private String serializeToString(Serializable serializable) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(serializable);
        oos.close();
        baos.close();
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private Serializable deserialize(String serializableString) throws IOException, ClassNotFoundException {
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