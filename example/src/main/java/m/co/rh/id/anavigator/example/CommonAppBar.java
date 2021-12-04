package m.co.rh.id.anavigator.example;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.Toolbar;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.annotation.NavInject;
import m.co.rh.id.anavigator.component.INavigator;

public class CommonAppBar extends StatefulView<Activity> implements Externalizable {

    @NavInject
    private transient INavigator mNavigator;

    private String mTitle;
    private transient View.OnClickListener mNavigationOnClickListener;
    private Integer mBackgroundColor;
    private Boolean mIsInitialRoute;

    /**
     * Used for Externalizable only
     */
    @Deprecated
    @VisibleForTesting
    public CommonAppBar() {
    }

    public CommonAppBar(boolean isInitialRoute) {
        mBackgroundColor = Color.BLUE;
        mIsInitialRoute = isInitialRoute;
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

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(mTitle);
        out.writeObject(mBackgroundColor);
        out.writeObject(mIsInitialRoute);
    }

    @Override
    public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
        super.readExternal(in);
        mTitle = (String) in.readObject();
        mBackgroundColor = (Integer) in.readObject();
        mIsInitialRoute = (Boolean) in.readObject();
    }
}
