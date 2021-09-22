package m.co.rh.id.anavigator.example;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;

import androidx.appcompat.widget.Toolbar;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;

public class CommonAppBar extends StatefulView<Activity> {

    private INavigator mNavigator;
    private String mTitle;
    private View.OnClickListener mNavigationOnClickListener;
    private boolean mIsInitialRoute;
    private int mBackgroundColor;

    public CommonAppBar(INavigator navigator) {
        mNavigator = navigator;
        mIsInitialRoute = navigator.isInitialRoute();
        mBackgroundColor = Color.BLUE;
    }

    @Override
    protected View createView(Activity activity) {
        View view = activity.getLayoutInflater().inflate(R.layout.common_app_bar, null, false);
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
