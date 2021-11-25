package m.co.rh.id.anavigator.example.dialog;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.RequireNavigator;
import m.co.rh.id.anavigator.example.R;

public class Full1Page extends StatefulView<Activity> implements RequireNavigator {

    private transient INavigator mNavigator;
    private int mCount;


    @Override
    public void provideNavigator(INavigator navigator) {
        mNavigator = navigator;
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_dialog_full_1, container, false);
        TextView textViewCount = view.findViewById(R.id.text_view_count);
        textViewCount.setText("Current count: " + mCount);
        Button buttonCount = view.findViewById(R.id.button_count);
        buttonCount.setOnClickListener(v -> {
            mCount++;
            textViewCount.setText("Current count: " + mCount);
        });
        Button buttonShowDialog2 = view.findViewById(R.id.button_show_dialog_2);
        buttonShowDialog2.setOnClickListener(v ->
                mNavigator.push((args, activity1) -> new Dialog2Page()));
        Toast.makeText(activity, "Dialog Full page 1 createView", Toast.LENGTH_LONG).show();
        return view;
    }
}
