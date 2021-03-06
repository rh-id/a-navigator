package m.co.rh.id.anavigator.example.dialog;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import m.co.rh.id.anavigator.StatefulViewDialog;
import m.co.rh.id.anavigator.example.R;
import m.co.rh.id.anavigator.example.Routes;
import m.co.rh.id.anavigator.exception.NavigationRouteNotFound;

public class Dialog2Page extends StatefulViewDialog<Activity> {

    private int mCount;

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View view = activity.getLayoutInflater().inflate(R.layout.page_dialog_2, container, false);
        TextView textViewCount = view.findViewById(R.id.text_view_count);
        textViewCount.setText("Current count: " + mCount);
        Button buttonCount = view.findViewById(R.id.button_count);
        buttonCount.setOnClickListener(v -> {
            mCount++;
            textViewCount.setText("Current count: " + mCount);
        });
        Button buttonRebuildAll = view.findViewById(R.id.button_rebuild_all);
        buttonRebuildAll.setOnClickListener(v ->
                getNavigator().reBuildAllRoute());
        Button buttonRebuild = view.findViewById(R.id.button_rebuild);
        buttonRebuild.setOnClickListener(v -> {
            try {
                getNavigator().reBuildRoute(mCount);
            } catch (NavigationRouteNotFound ex) {
                Toast.makeText(activity, "Route not found: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        Button buttonRetry = view.findViewById(R.id.button_retry);
        buttonRetry.setOnClickListener(v -> getNavigator().retry());
        Button buttonDismis = view.findViewById(R.id.button_dismiss);
        buttonDismis.setOnClickListener(v -> getNavigator().pop());
        Button buttonPopUntilHome = view.findViewById(R.id.button_popuntil_home);
        buttonPopUntilHome.setOnClickListener(v -> getNavigator().popUntil(Routes.HOME_PAGE, "pop until result"));
        Toast.makeText(activity, "Dialog page 2 createView", Toast.LENGTH_LONG).show();
        return view;
    }
}
