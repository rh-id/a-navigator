package m.co.rh.id.anavigator.example;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.Toolbar;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.RequireNavigator;

public class CommonAppBar extends StatefulView<Activity> implements RequireNavigator {

    private transient INavigator mNavigator;
    private String mTitle;
    private transient View.OnClickListener mNavigationOnClickListener;
    private int mBackgroundColor;
    private boolean mIsInitialRoute;

    public CommonAppBar(INavigator navigator) {
        mNavigator = navigator;
        mBackgroundColor = Color.BLUE;
        mIsInitialRoute = mNavigator.isInitialRoute();
    }

    @Override
    public void provideNavigator(INavigator navigator) {
        mNavigator = navigator;
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.common_app_bar, container, false);
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(mTitle);
        toolbar.setBackgroundColor(mBackgroundColor);
        if (mIsInitialRoute) {
            toolbar.setNavigationIcon(R.drawable.ic_navigation_menu);
            toolbar.setNavigationOnClickListener(mNavigationOnClickListener);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            toolbar.setNavigationOnClickListener(view1 -> mNavigator.pop());
        }

        return view;
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        mNavigationOnClickListener = null;
        mTitle = null;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setNavigationOnClickListener(View.OnClickListener navigationOnClickListener) {
        mNavigationOnClickListener = navigationOnClickListener;
    }

    public void setBackgroundColor(int backgroundColor) {
        mBackgroundColor = backgroundColor;
    }
}
