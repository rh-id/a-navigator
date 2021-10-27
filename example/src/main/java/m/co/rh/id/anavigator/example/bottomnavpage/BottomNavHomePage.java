package m.co.rh.id.anavigator.example.bottomnavpage;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import m.co.rh.id.anavigator.NavRoute;
import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.NavOnBackPressed;
import m.co.rh.id.anavigator.component.NavOnRouteChangedListener;
import m.co.rh.id.anavigator.component.RequireNavigator;
import m.co.rh.id.anavigator.example.R;
import m.co.rh.id.anavigator.example.Routes;


public class BottomNavHomePage extends StatefulView<Activity> implements RequireNavigator, NavOnBackPressed {
    private transient INavigator mNavigator;
    private transient INavigator mViewNavigator;
    private transient NavOnRouteChangedListener mNavOnRouteChangedListener;
    private int mSelectedId;


    @Override
    public void provideNavigator(INavigator navigator) {
        mNavigator = navigator;
        mViewNavigator = mNavigator.findViewNavigator(R.id.unique_container1);
    }

    @Override
    protected void initState(Activity activity) {
        super.initState(activity);
        mSelectedId = getSelectedId(mViewNavigator.getCurrentRoute());
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_home_bottomnav, container, false);
        BottomNavigationView bottomNavigationView = view.findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(mSelectedId);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            NavRoute navRoute = mViewNavigator.getCurrentRoute();
            String routeName = navRoute.getRouteName();
            int itemId = item.getItemId();
            if (itemId == R.id.menu_nav_home && !Routes.HOME_PAGE.equals(routeName)) {
                mViewNavigator.push(Routes.HOME_PAGE);
                return true;
            } else if (itemId == R.id.menu_nav_bottom_page1 && !Routes.PAGE_1.equals(routeName)) {
                mViewNavigator.push(Routes.PAGE_1);
                return true;
            } else if (itemId == R.id.menu_nav_bottom_page2 && !Routes.PAGE_2.equals(routeName)) {
                mViewNavigator.push(Routes.PAGE_2);
                return true;
            }
            return false;
        });
        if (mNavOnRouteChangedListener != null) {
            mViewNavigator.removeOnRouteChangedListener(mNavOnRouteChangedListener);
            mNavOnRouteChangedListener = null;
        }
        mNavOnRouteChangedListener = (previous, current) -> {
            mSelectedId = getSelectedId(current);
            bottomNavigationView.getMenu().findItem(mSelectedId).setChecked(true);
        };
        mViewNavigator.addOnRouteChangedListener(mNavOnRouteChangedListener);
        return view;
    }

    private int getSelectedId(NavRoute currentRoute) {
        int selectedId = R.id.menu_nav_home;
        if (currentRoute != null) {
            String routeName = currentRoute.getRouteName();
            if (Routes.PAGE_1.equals(routeName)) {
                selectedId = R.id.menu_nav_bottom_page1;
            } else if (Routes.PAGE_2.equals(routeName)) {
                selectedId = R.id.menu_nav_bottom_page2;
            }
        }
        return selectedId;
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        if (mNavOnRouteChangedListener != null) {
            INavigator viewNavigator = mNavigator.findViewNavigator(R.id.unique_container1);
            viewNavigator.removeOnRouteChangedListener(mNavOnRouteChangedListener);
            mNavOnRouteChangedListener = null;
        }
        mNavigator = null;
    }

    @Override
    public void onBackPressed(View currentView, Activity activity, INavigator navigator) {
        INavigator viewNavigator = navigator.findViewNavigator(R.id.unique_container1);
        if (viewNavigator.isInitialRoute()) {
            navigator.pop();
        } else {
            viewNavigator.pop();
        }
    }
}
