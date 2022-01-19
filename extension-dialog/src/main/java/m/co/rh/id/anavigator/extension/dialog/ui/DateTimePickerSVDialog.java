package m.co.rh.id.anavigator.extension.dialog.ui;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import m.co.rh.id.anavigator.NavRoute;
import m.co.rh.id.anavigator.StatefulViewDialog;
import m.co.rh.id.anavigator.component.RequireNavRoute;
import m.co.rh.id.anavigator.extension.dialog.R;

class DateTimePickerSVDialog extends StatefulViewDialog<Activity> implements RequireNavRoute, View.OnClickListener {

    private transient NavRoute mNavRoute;
    private transient Button mSetDateButton;
    private transient Button mSetTimeButton;
    private transient TimePicker mTimePicker;
    private transient DatePicker mDatePicker;

    @Override
    public void provideNavRoute(NavRoute navRoute) {
        mNavRoute = navRoute;
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View rootLayout = activity.getLayoutInflater().inflate(R.layout.a_navigator_extension_dialog_date_time_picker_dialog, container, false);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getDate());
        mSetDateButton = rootLayout.findViewById(R.id.button_set_date);
        mSetDateButton.setOnClickListener(this);
        mSetTimeButton = rootLayout.findViewById(R.id.button_set_time);
        mSetTimeButton.setOnClickListener(this);
        mDatePicker = rootLayout.findViewById(R.id.date_picker);
        mDatePicker.updateDate(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        mTimePicker = rootLayout.findViewById(R.id.time_picker);
        mTimePicker.setIs24HourView(is24HourFormat());
        mTimePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
        mTimePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
        Button okButton = rootLayout.findViewById(R.id.button_ok);
        okButton.setOnClickListener(this);
        Button cancelButton = rootLayout.findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(this);
        mSetDateButton.performClick();
        return rootLayout;
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        mSetDateButton = null;
        mSetTimeButton = null;
        mDatePicker = null;
        mTimePicker = null;
    }

    public boolean is24HourFormat() {
        Args args = Args.of(mNavRoute);
        if (args != null) {
            return args.mIs24HourFormat;
        }
        return true;
    }

    public Date getDate() {
        Args args = Args.of(mNavRoute);
        if (args != null) {
            return args.mDate;
        }
        return new Date();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_set_date) {
            mSetDateButton.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            mSetTimeButton.setTypeface(Typeface.DEFAULT);
            mDatePicker.setVisibility(View.VISIBLE);
            mTimePicker.setVisibility(View.GONE);
        } else if (id == R.id.button_set_time) {
            mSetDateButton.setTypeface(Typeface.DEFAULT);
            mSetTimeButton.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            mDatePicker.setVisibility(View.GONE);
            mTimePicker.setVisibility(View.VISIBLE);
        } else if (id == R.id.button_ok) {
            int hourOfDay = mTimePicker.getCurrentHour();
            int minute = mTimePicker.getCurrentMinute();
            int year = mDatePicker.getYear();
            int month = mDatePicker.getMonth();
            int day = mDatePicker.getDayOfMonth();
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day, hourOfDay, minute);
            Date date = calendar.getTime();
            getNavigator().pop(Result.newResult(date));
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

        private static Result newResult(Date date) {
            Result result = new Result();
            result.mDate = date;
            return result;
        }

        private Date mDate;

        public Date getDate() {
            return mDate;
        }
    }

    public static class Args implements Serializable {
        public static Args newArgs(boolean is24HourFormat, Date date) {
            Args args = new Args();
            args.mIs24HourFormat = is24HourFormat;
            if (date == null) {
                args.mDate = new Date();
            } else {
                args.mDate = date;
            }
            return args;
        }

        public static Args newArgs(boolean is24HourFormat) {
            Args args = new Args();
            args.mIs24HourFormat = is24HourFormat;
            args.mDate = new Date();
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

        private boolean mIs24HourFormat;
        private Date mDate;
    }
}
