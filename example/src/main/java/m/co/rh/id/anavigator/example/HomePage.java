package m.co.rh.id.anavigator.example;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.Serializable;

import m.co.rh.id.anavigator.Navigator;
import m.co.rh.id.anavigator.RouteOptions;
import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.NavOnActivityResult;
import m.co.rh.id.anavigator.component.NavOnBackPressed;
import m.co.rh.id.anavigator.component.RequireNavigator;
import m.co.rh.id.anavigator.example.dialog.DialogHomePage;


public class HomePage extends StatefulView<Activity> implements RequireNavigator, NavOnBackPressed, NavOnActivityResult {
    private static final int APPCOMPAT_ACTIVITY_REQUEST_CODE = 1;
    private CommonAppBar mCommonAppBar;
    private transient INavigator mNavigator;
    public boolean isDrawerOpen;

    @Override
    public void provideNavigator(INavigator navigator) {
        mNavigator = navigator;
        if (mCommonAppBar == null) {
            mCommonAppBar = new CommonAppBar(navigator);
        } else {
            mCommonAppBar.provideNavigator(navigator);
        }
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_home, container, false);
        DrawerLayout drawerLayout = view.findViewById(R.id.drawer);
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                // leave blank
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                isDrawerOpen = true;
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                isDrawerOpen = false;
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                // leave blank
            }
        });
        if (isDrawerOpen) {
            drawerLayout.open();
        }
        NavigationView navigationView = view.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home_appcompat) {
                activity.startActivityForResult(
                        new Intent(activity, AppCompatExampleActivity.class), APPCOMPAT_ACTIVITY_REQUEST_CODE);
            } else if (itemId == R.id.nav_second) {
                mNavigator.push(Routes.SECOND_PAGE, null, (activity1, currentView, result)
                        -> Toast.makeText(activity1, "Returned from second page with result: " + result, Toast.LENGTH_SHORT).show());
            } else if (itemId == R.id.nav_second_anonymous) {
                mNavigator.push((args, activity1) -> new SecondPage(), null, (activity1, currentView, result)
                                -> Toast.makeText(activity1, "Returned from second page anonymous route with result: " + result, Toast.LENGTH_SHORT).show(),
                        RouteOptions.withAnimation(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left,
                                android.R.anim.slide_in_left,
                                android.R.anim.slide_out_right
                        ));
            } else if (itemId == R.id.nav_bottom_home) {
                mNavigator.push(Routes.BOTTOM_NAV_PAGE);
            } else if (itemId == R.id.nav_dialog_home) {
                mNavigator.push((args, activity1) -> new DialogHomePage());
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
        appBarContainer.addView(mCommonAppBar.buildView(activity, appBarContainer));
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
    public void onBackPressed(View currentView, Activity activity, INavigator navigator) {
        DrawerLayout drawerLayout = currentView.findViewById(R.id.drawer);
        if (drawerLayout != null && drawerLayout.isOpen()) {
            drawerLayout.close();
        } else {
            navigator.pop();
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
