package m.co.rh.id.anavigator.example;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import m.co.rh.id.anavigator.NavConfiguration;
import m.co.rh.id.anavigator.Navigator;
import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.StatefulViewFactory;
import m.co.rh.id.anavigator.example.bottomnavpage.Bottom1Page;
import m.co.rh.id.anavigator.example.bottomnavpage.Bottom2Page;
import m.co.rh.id.anavigator.example.bottomnavpage.BottomHomePage;
import m.co.rh.id.anavigator.example.bottomnavpage.BottomNavHomePage;

public class MyApplication extends Application {

    public static MyApplication of(Context context) {
        if (context != null && context.getApplicationContext() instanceof MyApplication) {
            return (MyApplication) context.getApplicationContext();
        }
        return null;
    }

    private INavigator
            mRawActivityNavigator;

    private INavigator
            mAppCompatActivityNavigator;

    @Override
    public void onCreate() {
        super.onCreate();

        // Prepare navigator for RawActivity
        Map<String, StatefulViewFactory<RawActivity, StatefulView<Activity>>> navMap = new HashMap<>();
        navMap.put(Routes.HOME_PAGE, (args, activity) -> {
            if (args instanceof Boolean) {
                if ((Boolean) args) {
                    return new HomePage();
                }
            }
            return new SplashPage();
        });
        navMap.put(Routes.SECOND_PAGE, (args, activity) -> new SecondPage());
        navMap.put(Routes.BOTTOM_NAV_PAGE, (args, activity) -> new BottomNavHomePage());
        NavConfiguration.Builder<RawActivity, StatefulView<Activity>> navBuilder1 = new NavConfiguration.Builder<>(Routes.HOME_PAGE, navMap);
        navBuilder1.setSaveStateFile(new File(getCacheDir(), "anavigator/navigator1State"));

        NavConfiguration<RawActivity, StatefulView<Activity>> navConfiguration =
                navBuilder1.build();
        Navigator<RawActivity, StatefulView<Activity>> navigator =
                new Navigator<>(RawActivity.class, navConfiguration);
        // setup bottom nav pages
        Map<String, StatefulViewFactory<RawActivity, StatefulView<Activity>>> bottomPageMap = new HashMap<>();
        bottomPageMap.put(Routes.HOME_PAGE, (args, activity) -> new BottomHomePage());
        bottomPageMap.put(Routes.PAGE_1, (args, activity) -> new Bottom1Page());
        bottomPageMap.put(Routes.PAGE_2, (args, activity) -> new Bottom2Page());
        NavConfiguration.Builder<RawActivity, StatefulView<Activity>> navBuilderBottom = new NavConfiguration.Builder<>(Routes.HOME_PAGE, bottomPageMap);
        navBuilderBottom.setSaveStateFile(new File(getCacheDir(), "navigatorBottomState"));
        navigator.createViewNavigator(navBuilderBottom.build(), R.id.unique_container1);


        mRawActivityNavigator = navigator;
        // make sure to register navigator as callbacks to work properly
        registerActivityLifecycleCallbacks(navigator);
        registerComponentCallbacks(navigator);

        // Prepare navigator AppCompatExampleActivity
        Map<String, StatefulViewFactory<AppCompatExampleActivity, StatefulView>> navMap2 = new HashMap<>();
        navMap2.put(Routes.HOME_PAGE, (args, activity) -> new AppCompatHomePage());
        // can be re-used if needed
        navMap2.put(Routes.SECOND_PAGE, (args, activity) -> new SecondPage());
        NavConfiguration.Builder<AppCompatExampleActivity, StatefulView> navBuilder2 = new NavConfiguration.Builder<>(Routes.HOME_PAGE, navMap2);
        navBuilder2.setSaveStateFile(new File(getCacheDir(), "AppCompatNavState"));
        NavConfiguration<AppCompatExampleActivity, StatefulView> navConfiguration2 =
                navBuilder2.build();
        Navigator<AppCompatExampleActivity, StatefulView> navigator2 =
                new Navigator<>(AppCompatExampleActivity.class, navConfiguration2);
        mAppCompatActivityNavigator = navigator2;
        // make sure to register navigator as callbacks to work properly
        registerActivityLifecycleCallbacks(navigator2);
        registerComponentCallbacks(navigator2);
    }

    public INavigator
    getNavigator(Activity activity) {
        if (activity instanceof RawActivity) {
            return mRawActivityNavigator;
        } else if (activity instanceof AppCompatExampleActivity) {
            return mAppCompatActivityNavigator;
        }
        return null;
    }
}
