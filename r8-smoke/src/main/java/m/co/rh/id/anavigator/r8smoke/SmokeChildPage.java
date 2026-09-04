package m.co.rh.id.anavigator.r8smoke;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.annotation.NavInject;
import m.co.rh.id.anavigator.component.INavigator;

/**
 * Initial route of the nested ViewNavigator, injected into SmokePage's
 * R.id.smoke_container container.
 */
public class SmokeChildPage extends StatefulView<Activity> {

    @NavInject
    private transient INavigator mNavigator;

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        SmokeResult.childNavigatorInjected = mNavigator != null
                && mNavigator.getActivity() != null;
        TextView textView = new TextView(activity);
        textView.setText("SmokeChildPage");
        return textView;
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        mNavigator = null;
    }
}
