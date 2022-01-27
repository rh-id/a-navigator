package m.co.rh.id.anavigator.example.extension;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.Date;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.annotation.NavInject;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.example.MyApplication;
import m.co.rh.id.anavigator.example.R;
import m.co.rh.id.anavigator.extension.dialog.ui.NavExtDialogConfig;

public class ExtensionHomePage extends StatefulView<Activity> {

    @NavInject
    private transient INavigator mNavigator;

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        NavExtDialogConfig navExtDialogConfig = MyApplication.of(activity).getNavExtDialogConfig();
        View view = activity.getLayoutInflater().inflate(R.layout.page_extension_home, container, false);
        Button showMessageDialogButton = view.findViewById(R.id.button_show_message_dialog);
        showMessageDialogButton.setOnClickListener(v -> {
            String exampleTitle = "This is title";
            String exampleContent = "this is content";
            mNavigator.push(navExtDialogConfig.getRoutePath(NavExtDialogConfig.ROUTE_MESSAGE),
                    navExtDialogConfig.args_messageDialog(exampleTitle, exampleContent));
        });
        Button showConfirmDialogButton = view.findViewById(R.id.button_show_confirm_dialog);
        showConfirmDialogButton.setOnClickListener(v -> {
            String exampleTitle = "This is title";
            String exampleContent = "this is content";
            mNavigator.push(navExtDialogConfig.getRoutePath(NavExtDialogConfig.ROUTE_CONFIRM),
                    navExtDialogConfig.args_confirmDialog(exampleTitle, exampleContent),
                    (navigator, navRoute, activity1, currentView) -> {
                        Boolean result =
                                MyApplication.of(activity).getNavExtDialogConfig()
                                        .result_confirmDialog(navRoute);
                        Toast.makeText(activity1, "Return result: " + result, Toast.LENGTH_LONG)
                                .show();
                    });
        });
        Button showDateTimePickerDialogButton = view.findViewById(R.id.button_show_date_time_picker_dialog);
        showDateTimePickerDialogButton.setOnClickListener(v -> {
            mNavigator.push(navExtDialogConfig.getRoutePath(NavExtDialogConfig.ROUTE_DATE_TIME_PICKER),
                    navExtDialogConfig.args_dateTimePickerDialog(null, null),
                    (navigator, navRoute, activity1, currentView) -> {
                        Date result =
                                MyApplication.of(activity).getNavExtDialogConfig()
                                        .result_dateTimePickerDialog(navRoute);
                        Toast.makeText(activity1, "Return result: " + result, Toast.LENGTH_LONG)
                                .show();
                    });
        });
        Button showTimePickerDialogButton = view.findViewById(R.id.button_show_time_picker_dialog);
        showTimePickerDialogButton.setOnClickListener(v -> {
            mNavigator.push(navExtDialogConfig.getRoutePath(NavExtDialogConfig.ROUTE_TIME_PICKER),
                    navExtDialogConfig.args_timePickerDialog(true, 12, 0),
                    (navigator, navRoute, activity1, currentView) -> {
                        Integer hour =
                                MyApplication.of(activity).getNavExtDialogConfig()
                                        .result_timePickerDialog_hourOfDay(navRoute);
                        Integer min =
                                MyApplication.of(activity).getNavExtDialogConfig()
                                        .result_timePickerDialog_minute(navRoute);
                        Toast.makeText(activity1, "Return hour: " + hour + " minute: " + min, Toast.LENGTH_LONG)
                                .show();
                    });
        });
        return view;
    }
}
