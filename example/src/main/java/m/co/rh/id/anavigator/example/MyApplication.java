package m.co.rh.id.anavigator.example;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.view.animation.AnimationUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

import m.co.rh.id.anavigator.NavConfiguration;
import m.co.rh.id.anavigator.Navigator;
import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.StatefulViewFactory;
import m.co.rh.id.anavigator.example.bottomnavpage.Bottom1Page;
import m.co.rh.id.anavigator.example.bottomnavpage.Bottom2Page;
import m.co.rh.id.anavigator.example.bottomnavpage.BottomHomePage;
import m.co.rh.id.anavigator.example.bottomnavpage.BottomNavHomePage;
import m.co.rh.id.anavigator.example.component.ExampleComponent;
import m.co.rh.id.anavigator.extension.dialog.ui.NavExtDialogConfig;

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

    private NavExtDialogConfig mNavExtDialogConfig;

    @Override
    public void onCreate() {
        super.onCreate();

        // Prepare navigator for RawActivity
        Map<String, StatefulViewFactory> navMap = new HashMap<>();
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
        // configure extension-dialog
        mNavExtDialogConfig = new NavExtDialogConfig(this);
        navMap.putAll(mNavExtDialogConfig.getNavMap());
        NavConfiguration.Builder<Activity, StatefulView> navBuilder1 = new NavConfiguration.Builder(Routes.HOME_PAGE, navMap);
        navBuilder1.setSaveStateFile(new File(getCacheDir(), "anavigator/navigator1State"));
        // example cipher
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA1");
            PBEKeySpec specKey = new PBEKeySpec("password".toCharArray(), "salt".getBytes(), 1
                    , 256);
            SecretKey key = factory.generateSecret(specKey);
            byte[] iv = new byte[]{
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
            };
            Cipher encryptCipher = Cipher.getInstance("PBEWITHSHA256AND256BITAES-CBC-BC");
            encryptCipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            Cipher decryptCipher = Cipher.getInstance("PBEWITHSHA256AND256BITAES-CBC-BC");
            decryptCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            navBuilder1.setSaveStateCipher(encryptCipher, decryptCipher);
        } catch (Throwable throwable) {
            Log.e("MyApplication", "Failed setting encrypted navigator", throwable);
        }

        navBuilder1.setRequiredComponent(new ExampleComponent("this is sample component"));
        NavConfiguration<Activity, StatefulView> navConfiguration =
                navBuilder1.build();
        Navigator<Activity, StatefulView> navigator =
                new Navigator(RawActivity.class, navConfiguration);
        // setup bottom nav pages
        Map<String, StatefulViewFactory<RawActivity, StatefulView<Activity>>> bottomPageMap = new HashMap<>();
        bottomPageMap.put(Routes.HOME_PAGE, (args, activity) -> new BottomHomePage());
        bottomPageMap.put(Routes.PAGE_1, (args, activity) -> new Bottom1Page());
        bottomPageMap.put(Routes.PAGE_2, (args, activity) -> new Bottom2Page());
        NavConfiguration.Builder<RawActivity, StatefulView<Activity>> navBuilderBottom = new NavConfiguration.Builder<>(Routes.HOME_PAGE, bottomPageMap);
        navBuilderBottom.setSaveStateFile(new File(getCacheDir(), "navigatorBottomState"));
        navBuilderBottom.setAnimation(
                AnimationUtils.loadAnimation(this, R.anim.slide_in_right),
                AnimationUtils.loadAnimation(this, R.anim.slide_out_left),
                AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left),
                AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right)
        );
        navigator.createViewNavigator(navBuilderBottom.build(), R.id.unique_container1);


        mRawActivityNavigator = navigator;
        // make sure to register navigator as callbacks to work properly
        registerActivityLifecycleCallbacks(navigator);
        registerComponentCallbacks(navigator);

        // Prepare navigator AppCompatExampleActivity
        Map<String, StatefulViewFactory> navMap2 = new HashMap<>();
        navMap2.put(Routes.HOME_PAGE, (args, activity) -> new AppCompatHomePage());
        // can be re-used if needed
        navMap2.put(Routes.SECOND_PAGE, (args, activity) -> new SecondPage());
        navMap2.putAll(mNavExtDialogConfig.getNavMap());
        NavConfiguration.Builder<Activity, StatefulView> navBuilder2 = new NavConfiguration.Builder(Routes.HOME_PAGE, navMap2);
        navBuilder2.setSaveStateFile(new File(getCacheDir(), "AppCompatNavState"));
        NavConfiguration<Activity, StatefulView> navConfiguration2 =
                navBuilder2.build();
        Navigator<Activity, StatefulView> navigator2 =
                new Navigator(AppCompatExampleActivity.class, navConfiguration2);
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

    public NavExtDialogConfig getNavExtDialogConfig() {
        return mNavExtDialogConfig;
    }
}
