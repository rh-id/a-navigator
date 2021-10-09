package m.co.rh.id.anavigator.example.bottomnavpage;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.RequireNavigator;
import m.co.rh.id.anavigator.example.R;

public class BottomHomePage extends StatefulView<Activity> implements RequireNavigator {
    private transient INavigator mNavigator;
    private int counter;

    @Override
    public void provideNavigator(INavigator navigator) {
        mNavigator = navigator;
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_bottomnav_home, container, false);
        TextView textView = view.findViewById(R.id.textView);
        textView.setText(activity.getString(R.string.counter, counter));
        MaterialButton plusButton = view.findViewById(R.id.button_plus);
        plusButton.setOnClickListener(view12 -> {
            counter++;
            textView.setText(activity.getString(R.string.counter, counter));
        });
        MaterialButton returnButton = view.findViewById(R.id.button_return);
        returnButton.setOnClickListener(view1 ->
                mNavigator.pop(counter));
        return view;
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        mNavigator = null;
    }
}
