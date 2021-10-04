package m.co.rh.id.anavigator.example;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.RequireNavigator;

public class ThirdPage extends StatefulView<Activity> implements RequireNavigator {
    private transient INavigator mNavigator;
    private CommonAppBar mCommonAppBar;
    private int counter;

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
    protected void initState(Activity activity) {
        super.initState(activity);
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_third, container, false);
        mCommonAppBar.setTitle(activity.getString(R.string.third_page_title));
        ViewGroup appBar = view.findViewById(R.id.container_app_bar);
        appBar.addView(mCommonAppBar.buildView(activity, appBar));
        TextView textView = view.findViewById(R.id.textview_third);
        textView.setText(activity.getString(R.string.third_page_title));
        return view;
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        mCommonAppBar.dispose(activity);
        mCommonAppBar = null;
        mNavigator = null;
    }
}
