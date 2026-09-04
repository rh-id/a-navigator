package m.co.rh.id.anavigator.r8smoke;

import android.app.Activity;
import android.content.Context;
import android.app.Application;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import m.co.rh.id.anavigator.NavConfiguration;
import m.co.rh.id.anavigator.Navigator;
import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.StatefulViewFactory;

/**
 * Registers the NavConfiguration exactly like a real consumer app would
 * (mirrors the example module's MyApplication).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SmokeApplication extends Application {

    public static final String ROUTE_LANDING = "/";
    public static final String ROUTE_SMOKE = "/smoke";

    private INavigator mNavigator;

    public static SmokeApplication of(Context context) {
        if (context != null && context.getApplicationContext() instanceof SmokeApplication) {
            return (SmokeApplication) context.getApplicationContext();
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Each app start must behave like a fresh consumer install: the navigator
        // persists its route stack under cacheDir/m.co.rh.id.anavigator and would
        // restore it (re-running injections with stale route stacks) instead of
        // pushing the initial route again.
        deleteNavigatorStateDir();

        Map<String, StatefulViewFactory<MainActivity, StatefulView<Activity>>> navMap =
                new HashMap<>();
        navMap.put(ROUTE_LANDING, (args, activity) -> new LandingPage());
        navMap.put(ROUTE_SMOKE, (args, activity) -> new SmokePage());
        NavConfiguration.Builder<MainActivity, StatefulView<Activity>> navBuilder =
                new NavConfiguration.Builder<>(ROUTE_LANDING, navMap);
        // component injection target for @NavInject ISmokeComponent fields
        navBuilder.setRequiredComponent(new SmokeComponent());
        NavConfiguration<MainActivity, StatefulView<Activity>> navConfiguration =
                navBuilder.build();
        Navigator<MainActivity, StatefulView<Activity>> navigator =
                new Navigator<>(MainActivity.class, navConfiguration);

        // Nested view navigator rendered into SmokePage's R.id.smoke_container
        // (mirrors the example module's bottom navigation setup).
        Map<String, StatefulViewFactory<MainActivity, StatefulView<Activity>>> childMap
                = new HashMap<>();
        childMap.put(ROUTE_LANDING, (args, activity) -> new SmokeChildPage());
        NavConfiguration.Builder<MainActivity, StatefulView<Activity>> childBuilder =
                new NavConfiguration.Builder<>(ROUTE_LANDING, childMap);
        navigator.createViewNavigator(childBuilder.build(), R.id.smoke_container);

        mNavigator = navigator;
        // make sure to register navigator as callbacks to work properly
        registerActivityLifecycleCallbacks(navigator);
        registerComponentCallbacks(navigator);
    }

    public INavigator getNavigator(Activity activity) {
        if (activity instanceof MainActivity) {
            return mNavigator;
        }
        return null;
    }

    private void deleteNavigatorStateDir() {
        deleteRecursive(new File(getCacheDir(), "m.co.rh.id.anavigator"));
    }

    private static void deleteRecursive(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursive(child);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
