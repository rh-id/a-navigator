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
import m.co.rh.id.anavigator.extension.dialog.ui.ProgressSVDialog;

public class ExtensionHomePage extends StatefulView<Activity> {

    private static final long PROGRESS_TICK_MS = 100;
    private static final int PROGRESS_TICK_MAX = 100;

    @NavInject
    private transient INavigator mNavigator;
    // non-transient on purpose: the typed Args identity must survive process
    // death via single-graph serialization with the navigator snapshot, so
    // the page keeps streaming updates to the restored dialog
    private ProgressSVDialog.Args mProgressDialogArgs;
    private transient View mProgressTickerView;
    private transient Runnable mProgressTicker;

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        NavExtDialogConfig navExtDialogConfig = MyApplication.of(activity).getNavExtDialogConfig();
        View view = activity.getLayoutInflater().inflate(R.layout.page_extension_home, container, false);
        Button showMessageDialogButton = view.findViewById(R.id.button_show_message_dialog);
        showMessageDialogButton.setOnClickListener(v -> {
            String exampleTitle = "This is title";
            String exampleContent = "this is content";
            mNavigator.push(navExtDialogConfig.route_messageDialog(),
                    navExtDialogConfig.args_messageDialog(exampleTitle, exampleContent));
        });
        Button showConfirmDialogButton = view.findViewById(R.id.button_show_confirm_dialog);
        showConfirmDialogButton.setOnClickListener(v -> {
            String exampleTitle = "This is title";
            String exampleContent = "this is content";
            mNavigator.push(navExtDialogConfig.route_confirmDialog(),
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
            mNavigator.push(navExtDialogConfig.route_dateTimePickerDialog(),
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
            mNavigator.push(navExtDialogConfig.route_timePickerDialog(),
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
        Button showDatePickerDialogButton = view.findViewById(R.id.button_show_date_picker_dialog);
        showDatePickerDialogButton.setOnClickListener(v -> {
            mNavigator.push(navExtDialogConfig.route_datePickerDialog(),
                    navExtDialogConfig.args_datePickerDialog(new Date()),
                    (navigator, navRoute, activity1, currentView) -> {
                        Integer year =
                                MyApplication.of(activity).getNavExtDialogConfig()
                                        .result_datePickerDialog_year(navRoute);
                        Integer month =
                                MyApplication.of(activity).getNavExtDialogConfig()
                                        .result_datePickerDialog_month(navRoute);
                        Integer dayOfMonth =
                                MyApplication.of(activity).getNavExtDialogConfig()
                                        .result_datePickerDialog_dayOfMonth(navRoute);
                        Toast.makeText(activity1, "Return year: " + year + " month: " + month + " dayOfMonth: " + dayOfMonth, Toast.LENGTH_LONG)
                                .show();
                    });
        });
        Button showProgressDialogButton = view.findViewById(R.id.button_show_progress_dialog);
        showProgressDialogButton.setOnClickListener(v -> {
            mProgressDialogArgs = navExtDialogConfig.args_progressDialog("Progress demo", "Working… 0%", 100);
            mNavigator.push(navExtDialogConfig.route_progressDialog(), mProgressDialogArgs);
            startProgressTicker(v);
        });
        return view;
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        stopProgressTicker();
        mProgressDialogArgs = null;
        mNavigator = null;
    }

    /**
     * Demo ticker that streams fake progress into the progress dialog Args
     * every {@link #PROGRESS_TICK_MS} milliseconds and pops the dialog route
     * once it reaches {@link #PROGRESS_TICK_MAX} (the caller-pops contract).
     */
    private void startProgressTicker(View view) {
        stopProgressTicker();
        mProgressTickerView = view;
        mProgressTicker = new Runnable() {
            private int mProgress;

            @Override
            public void run() {
                mProgress++;
                if (mProgressDialogArgs != null) {
                    mProgressDialogArgs.updateProgress(mProgress);
                    mProgressDialogArgs.updateMessage("Working… " + mProgress + "%");
                }
                if (mProgress >= PROGRESS_TICK_MAX) {
                    mProgressTicker = null;
                    if (mNavigator != null) {
                        mNavigator.pop();
                    }
                    return;
                }
                if (mProgressTickerView != null) {
                    mProgressTickerView.postDelayed(this, PROGRESS_TICK_MS);
                }
            }
        };
        mProgressTickerView.postDelayed(mProgressTicker, PROGRESS_TICK_MS);
    }

    private void stopProgressTicker() {
        if (mProgressTickerView != null && mProgressTicker != null) {
            mProgressTickerView.removeCallbacks(mProgressTicker);
        }
        mProgressTickerView = null;
        mProgressTicker = null;
    }
}
