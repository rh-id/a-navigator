package m.co.rh.id.anavigator.example;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.Toolbar;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.annotation.NavInject;
import m.co.rh.id.anavigator.annotation.NavRouteIndex;
import m.co.rh.id.anavigator.component.INavigator;

public class CommonAppBar extends StatefulView<Activity> implements Externalizable {

    private static final String TAG = CommonAppBar.class.getName();
    @NavInject
    private transient INavigator mNavigator;
    @NavRouteIndex
    private transient Byte mRouteIndexByte;
    @NavRouteIndex
    private transient Short mRouteIndexShort;
    @NavRouteIndex
    private transient Integer mRouteIndexInteger;
    @NavRouteIndex
    private transient Long mRouteIndexLong;
    @NavRouteIndex
    private transient Float mRouteIndexFloat;
    @NavRouteIndex
    private transient Double mRouteIndexDouble;
    @NavRouteIndex
    private transient byte mRouteIndexPByte;
    @NavRouteIndex
    private transient short mRouteIndexPShort;
    @NavRouteIndex
    private transient int mRouteIndexPInteger;
    @NavRouteIndex
    private transient long mRouteIndexPLong;
    @NavRouteIndex
    private transient float mRouteIndexPFloat;
    @NavRouteIndex
    private transient double mRouteIndexPDouble;

    private String mTitle;
    private transient View.OnClickListener mNavigationOnClickListener;
    private Integer mBackgroundColor;

    public CommonAppBar() {
        mBackgroundColor = Color.BLUE;
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.common_app_bar, container, false);
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(mTitle);
        toolbar.setBackgroundColor(mBackgroundColor);
        if (isInitialRoute()) {
            toolbar.setNavigationIcon(R.drawable.ic_navigation_menu);
            toolbar.setNavigationOnClickListener(mNavigationOnClickListener);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            toolbar.setNavigationOnClickListener(view1 -> mNavigator.pop());
        }
        printRouteIndex();
        return view;
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        mNavigationOnClickListener = null;
        mTitle = null;
    }

    private void printRouteIndex() {
        Log.d(TAG, "mRouteIndexByte=" + mRouteIndexByte);
        Log.d(TAG, "mRouteIndexShort=" + mRouteIndexShort);
        Log.d(TAG, "mRouteIndexInteger=" + mRouteIndexInteger);
        Log.d(TAG, "mRouteIndexLong=" + mRouteIndexLong);
        Log.d(TAG, "mRouteIndexFloat=" + mRouteIndexFloat);
        Log.d(TAG, "mRouteIndexDouble=" + mRouteIndexDouble);
        // primitives
        Log.d(TAG, "mRouteIndexPByte=" + mRouteIndexPByte);
        Log.d(TAG, "mRouteIndexPShort=" + mRouteIndexPShort);
        Log.d(TAG, "mRouteIndexPInteger=" + mRouteIndexPInteger);
        Log.d(TAG, "mRouteIndexPLong=" + mRouteIndexPLong);
        Log.d(TAG, "mRouteIndexPFloat=" + mRouteIndexPFloat);
        Log.d(TAG, "mRouteIndexPDouble=" + mRouteIndexPDouble);
    }

    private boolean isInitialRoute() {
        return mRouteIndexInteger == 0;
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
    }

    @Override
    public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
        super.readExternal(in);
        mTitle = (String) in.readObject();
        mBackgroundColor = (Integer) in.readObject();
    }
}
