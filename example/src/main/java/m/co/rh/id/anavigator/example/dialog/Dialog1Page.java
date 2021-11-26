package m.co.rh.id.anavigator.example.dialog;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import m.co.rh.id.anavigator.StatefulViewDialog;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.example.R;

public class Dialog1Page extends StatefulViewDialog<Activity> {

    private int mCount;

    public Dialog1Page(INavigator navigator) {
        super(navigator);
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_dialog_1, container, false);
        TextView textViewCount = view.findViewById(R.id.text_view_count);
        textViewCount.setText("Current count: " + mCount);
        Button buttonCount = view.findViewById(R.id.button_count);
        buttonCount.setOnClickListener(v -> {
            mCount++;
            textViewCount.setText("Current count: " + mCount);
        });
        Button buttonShowPage2 = view.findViewById(R.id.button_show_page_1);
        buttonShowPage2.setOnClickListener(v ->
                getNavigator().push((args, activity1) -> new Full1Page()));
        Toast.makeText(activity, "Dialog page 1 createView", Toast.LENGTH_LONG).show();
        return view;
    }
}
