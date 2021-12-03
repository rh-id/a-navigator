package m.co.rh.id.anavigator.example.component;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.annotation.NavInject;
import m.co.rh.id.anavigator.component.INavigator;

/**
 * Dummy stateful view to test NavInject
 */
public class ExampleInjectedStatefulView extends StatefulView<Activity> {

    @NavInject
    private transient INavigator mNavigator;

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        return null;
    }

    @Override
    public String toString() {
        return super.toString() + ", mNavigator=" + mNavigator;
    }
}
