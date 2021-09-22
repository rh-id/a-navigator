package m.co.rh.id.anavigator.example;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.Serializable;

import m.co.rh.id.anavigator.Navigator;
import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.NavOnActivityResult;
import m.co.rh.id.anavigator.component.NavOnBackPressed;
import m.co.rh.id.anavigator.component.RequireNavigator;


public class HomePage extends StatefulView<Activity> implements RequireNavigator, NavOnBackPressed, NavOnActivityResult {
    private static final int APPCOMPAT_ACTIVITY_REQUEST_CODE = 1;
    private CommonAppBar mCommonAppBar;
    private INavigator mNavigator;

    @Override
    public void provideNavigator(INavigator navigator) {
        mNavigator = navigator;
        mCommonAppBar = new CommonAppBar(navigator);
    }

    @Override
    protected View createView(Activity activity) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_home, null, false);
        DrawerLayout drawerLayout = view.findViewById(R.id.drawer);
        NavigationView navigationView = view.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_home_appcompat:
                    activity.startActivityForResult(
                            new Intent(activity, AppCompatExampleActivity.class), APPCOMPAT_ACTIVITY_REQUEST_CODE);
                    break;
                case R.id.nav_second:
                    mNavigator.push(Routes.SECOND_PAGE);
                    break;
            }
            return false;
        });
        mCommonAppBar.setTitle(activity.getString(R.string.home_page));
        mCommonAppBar.setNavigationOnClickListener(view1 -> {
            if (!drawerLayout.isOpen()) {
                drawerLayout.open();
            }
        });
        ViewGroup appBarContainer = view.findViewById(R.id.container_app_bar);
        appBarContainer.addView(mCommonAppBar.buildView(activity));
        return view;
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        mCommonAppBar.dispose(activity);
        mCommonAppBar = null;
        mNavigator = null;
    }

    @Override
    public void onBackPressed(View currentView, Activity activity, INavigator INavigator) {
        DrawerLayout drawerLayout = currentView.findViewById(R.id.drawer);
        if (drawerLayout != null && drawerLayout.isOpen()) {
            drawerLayout.close();
        } else {
            INavigator.pop();
        }
    }

    @Override
    public void onActivityResult(View currentView, Activity activity, INavigator INavigator, int requestCode, int resultCode, Intent data) {
        String message = "Result from AppCompat Activity: ";
        if (resultCode == Activity.RESULT_OK) {
            Serializable serializable = data.getSerializableExtra(Navigator.ACTIVITY_RESULT_SERIALIZABLE_KEY);
            message += serializable;
        } else {
            message += "NOTHING RETURNED";
        }
        Snackbar.make(currentView,
                message,
                BaseTransientBottomBar.LENGTH_LONG).show();
    }
}
