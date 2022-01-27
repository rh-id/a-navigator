package m.co.rh.id.anavigator.extension.dialog.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TimePicker;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import m.co.rh.id.anavigator.NavRoute;
import m.co.rh.id.anavigator.StatefulViewDialog;
import m.co.rh.id.anavigator.component.RequireNavRoute;
import m.co.rh.id.anavigator.extension.dialog.R;

class TimePickerSVDialog extends StatefulViewDialog<Activity> implements RequireNavRoute, View.OnClickListener {

    private transient NavRoute mNavRoute;
    private transient TimePicker mTimePicker;

    @Override
    public void provideNavRoute(NavRoute navRoute) {
        mNavRoute = navRoute;
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View rootLayout = activity.getLayoutInflater().inflate(R.layout.a_navigator_extension_dialog_time_picker_dialog, container, false);
        mTimePicker = rootLayout.findViewById(R.id.time_picker);
        mTimePicker.setIs24HourView(is24HourFormat());
        mTimePicker.setCurrentHour(getHourOfDay());
        mTimePicker.setCurrentMinute(getMinute());
        Button okButton = rootLayout.findViewById(R.id.button_ok);
        okButton.setOnClickListener(this);
        Button cancelButton = rootLayout.findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(this);
        return rootLayout;
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        mTimePicker = null;
    }

    public boolean is24HourFormat() {
        Args args = Args.of(mNavRoute);
        if (args != null && args.mIs24HourFormat != null) {
            return args.mIs24HourFormat;
        }
        return true;
    }

    private Integer getHourOfDay() {
        Args args = Args.of(mNavRoute);
        if (args != null && args.mHourOfDay != null) {
            return args.mHourOfDay % 24;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    private Integer getMinute() {
        Args args = Args.of(mNavRoute);
        if (args != null && args.mMinute != null) {
            return args.mMinute % 60;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.MINUTE);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_ok) {
            int hourOfDay = mTimePicker.getCurrentHour();
            int minute = mTimePicker.getCurrentMinute();
            getNavigator().pop(Result.newResult(hourOfDay, minute));
        } else if (id == R.id.button_cancel) {
            getNavigator().pop();
        }
    }

    public static class Result implements Serializable {
        public static Result of(NavRoute navRoute) {
            if (navRoute != null) {
                return of(navRoute.getRouteResult());
            }
            return null;
        }

        public static Result of(Serializable serializable) {
            if (serializable instanceof Result) {
                return (Result) serializable;
            }
            return null;
        }

        private static Result newResult(int hourOfDay, int minute) {
            Result result = new Result();
            result.mHourOfDay = hourOfDay;
            result.mMinute = minute;
            return result;
        }

        private Integer mHourOfDay;
        private Integer mMinute;

        public Integer getHourOfDay() {
            return mHourOfDay;
        }

        public Integer getMinute() {
            return mMinute;
        }
    }

    public static class Args implements Serializable {
        public static Args newArgs(Boolean is24HourFormat, Integer hourOfDay, Integer minute) {
            Args args = new Args();
            args.mIs24HourFormat = is24HourFormat;
            args.mHourOfDay = hourOfDay;
            args.mMinute = minute;
            return args;
        }

        public static Args of(NavRoute navRoute) {
            if (navRoute != null) {
                return of(navRoute.getRouteArgs());
            }
            return null;
        }

        public static Args of(Serializable serializable) {
            if (serializable instanceof Args) {
                return (Args) serializable;
            }
            return null;
        }

        private Boolean mIs24HourFormat;
        private Integer mHourOfDay;
        private Integer mMinute;
    }
}
