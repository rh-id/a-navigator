package m.co.rh.id.anavigator.example.dialog;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.annotation.NavInject;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.example.R;

public class DialogHomePage extends StatefulView<Activity> {

    @NavInject
    private transient INavigator mNavigator;

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_dialog_home, container, false);
        Button dialog1Button = view.findViewById(R.id.button_show_dialog_1);
        dialog1Button.setOnClickListener(v -> mNavigator.push((args, activity1) -> new Dialog1Page()));
        return view;
    }
}
