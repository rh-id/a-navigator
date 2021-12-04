package m.co.rh.id.anavigator.example;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.annotation.NavInject;
import m.co.rh.id.anavigator.component.INavigator;

public class SecondPage extends StatefulView<Activity> {

    @NavInject
    private transient INavigator mNavigator;

    @NavInject
    private CommonAppBar mCommonAppBar;
    private int counter;

    public SecondPage() {
        mCommonAppBar = new CommonAppBar(false);
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_second, container, false);
        mCommonAppBar.setTitle(activity.getString(R.string.second_page_title));
        ViewGroup appBar = view.findViewById(R.id.container_app_bar);
        appBar.addView(mCommonAppBar.buildView(activity, appBar));
        TextView textView = view.findViewById(R.id.textview_second);
        textView.setText(activity.getString(R.string.second_page, counter));
        MaterialButton plusButton = view.findViewById(R.id.button_plus);
        plusButton.setOnClickListener(view12 -> {
            counter++;
            textView.setText(activity.getString(R.string.second_page, counter));
        });
        MaterialButton returnButton = view.findViewById(R.id.button_return);
        returnButton.setOnClickListener(view1 ->
                mNavigator.pop(counter));
        MaterialButton thirdButton = view.findViewById(R.id.button_third);
        thirdButton.setOnClickListener(view1 ->
                mNavigator.push((args, activity1) -> new ThirdPage()));
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
