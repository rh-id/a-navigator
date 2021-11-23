package m.co.rh.id.anavigator.example.bottomnavpage;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.util.UUID;
import java.util.regex.Pattern;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.RequireNavigator;
import m.co.rh.id.anavigator.example.PageKeys;
import m.co.rh.id.anavigator.example.R;
import m.co.rh.id.anavigator.exception.NavigationRouteNotFound;

public class Bottom2Page extends StatefulView<Activity> implements RequireNavigator {
    private transient INavigator mNavigator;
    private int counter;

    public Bottom2Page() {
        super(PageKeys.BOTTOM_PAGE + "-2-" + UUID.randomUUID().toString());
    }

    @Override
    public void provideNavigator(INavigator navigator) {
        mNavigator = navigator;
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_bottomnav_page2, container, false);
        TextView textView = view.findViewById(R.id.textView);
        textView.setText(activity.getString(R.string.bottom_page_2) + " "
                + activity.getString(R.string.counter, counter));
        MaterialButton plusButton = view.findViewById(R.id.button_plus);
        plusButton.setOnClickListener(view12 -> {
            counter++;
            textView.setText(activity.getString(R.string.bottom_page_2) + " "
                    + activity.getString(R.string.counter, counter));
        });
        MaterialButton returnButton = view.findViewById(R.id.button_return);
        returnButton.setOnClickListener(view1 ->
                mNavigator.pop(counter));
        MaterialButton rebuildButton = view.findViewById(R.id.button_rebuild);
        rebuildButton.setOnClickListener(v -> {
            try {
                mNavigator.reBuildRoute(counter);
            } catch (NavigationRouteNotFound e) {
                Toast.makeText(activity, e.getMessage(),
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });
        MaterialButton rebuildPatternButton = view.findViewById(R.id.button_rebuild_pattern_bottom_page);
        rebuildPatternButton.setOnClickListener(v ->
                mNavigator.reBuildRoute(Pattern.compile("^" + PageKeys.BOTTOM_PAGE)));

        Toast.makeText(activity, activity.getString(R.string.bottom_page_2),
                Toast.LENGTH_SHORT)
                .show();
        return view;
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        mNavigator = null;
    }
}
