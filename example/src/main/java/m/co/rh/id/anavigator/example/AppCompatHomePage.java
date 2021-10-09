package m.co.rh.id.anavigator.example;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.NavOnBackPressed;
import m.co.rh.id.anavigator.component.RequireNavigator;


public class AppCompatHomePage extends StatefulView<AppCompatActivity> implements RequireNavigator, NavOnBackPressed<AppCompatActivity> {

    private CommonAppBar mCommonAppBar;
    private transient INavigator mNavigator;

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
    protected View createView(AppCompatActivity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_appcompat_home, container, false);
        DrawerLayout drawerLayout = view.findViewById(R.id.drawer);
        NavigationView navigationView = view.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_second) {
                mNavigator.push(Routes.SECOND_PAGE);
            }
            return false;
        });
        mCommonAppBar.setTitle(activity.getString(R.string.appcompat_page));
        mCommonAppBar.setBackgroundColor(Color.RED);
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
    public void dispose(AppCompatActivity activity) {
        super.dispose(activity);
        mCommonAppBar.dispose(activity);
        mCommonAppBar = null;
        mNavigator = null;
    }

    @Override
    public void onBackPressed(View currentView, AppCompatActivity activity, INavigator navigator) {
        DrawerLayout drawerLayout = currentView.findViewById(R.id.drawer);
        if (drawerLayout != null && drawerLayout.isOpen()) {
            drawerLayout.close();
        } else {
            navigator.finishActivity("This is result from AppCompatHomePage");
        }
    }
}
