package m.co.rh.id.anavigator.example;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.RequireNavigator;


public class SplashPage extends StatefulView<Activity> implements RequireNavigator {
    private transient INavigator mNavigator;

    @Override
    public void provideNavigator(INavigator navigator) {
        mNavigator = navigator;
        // example simple timer, show 3 seconds then out
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            // The retry is done in provideNavigator instead of initState
            // this is because if saveState is enabled, this page will be restored
            // and provideNavigator will be executed but not initState
            // if this is not invoked then the screen will stuck on SplashPage
            mNavigator.retry(true); // splash showed to user and done
        }, 3000);
    }


    @Override
    protected View createView(Activity activity, ViewGroup container) {
        return activity.getLayoutInflater().inflate(R.layout.page_splash, container, false);
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        mNavigator = null;
    }
}
