package m.co.rh.id.anavigator.example;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.RequireNavigator;

public class SecondPage extends StatefulView<Activity> implements RequireNavigator {

    private INavigator mNavigator;
    private CommonAppBar mCommonAppBar;

    @Override
    public void provideNavigator(INavigator navigator) {
        mNavigator = navigator;
        mCommonAppBar = new CommonAppBar(navigator);
    }

    @Override
    protected void initState(Activity activity) {
        super.initState(activity);
        mCommonAppBar.setTitle(activity.getString(R.string.second_page));
    }

    @Override
    protected View createView(Activity activity) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_second, null, false);
        ViewGroup appBar = view.findViewById(R.id.container_app_bar);
        appBar.addView(mCommonAppBar.buildView(activity));
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
