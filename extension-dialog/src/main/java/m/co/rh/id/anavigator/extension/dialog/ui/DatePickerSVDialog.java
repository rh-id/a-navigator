package m.co.rh.id.anavigator.extension.dialog.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import m.co.rh.id.anavigator.NavRoute;
import m.co.rh.id.anavigator.StatefulViewDialog;
import m.co.rh.id.anavigator.component.RequireNavRoute;
import m.co.rh.id.anavigator.extension.dialog.R;

class DatePickerSVDialog extends StatefulViewDialog<Activity> implements RequireNavRoute, View.OnClickListener {

    private transient NavRoute mNavRoute;
    private transient DatePicker mDatePicker;

    @Override
    public void provideNavRoute(NavRoute navRoute) {
        mNavRoute = navRoute;
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View rootLayout = activity.getLayoutInflater().inflate(R.layout.a_navigator_extension_dialog_date_picker_dialog, container, false);
        mDatePicker = rootLayout.findViewById(R.id.date_picker);
        mDatePicker.updateDate(getYear(),
                getMonth(),
                getDayOfMonth());
        Button okButton = rootLayout.findViewById(R.id.button_ok);
        okButton.setOnClickListener(this);
        Button cancelButton = rootLayout.findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(this);
        return rootLayout;
    }

    @Override
    public void dispose(Activity activity) {
        super.dispose(activity);
        mDatePicker = null;
    }

    public int getYear() {
        Args args = Args.of(mNavRoute);
        if (args != null && args.mYear != null) {
            return args.mYear;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.YEAR);
    }

    public int getMonth() {
        Args args = Args.of(mNavRoute);
        if (args != null && args.mMonth != null) {
            return args.mMonth;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.MONTH);
    }

    public int getDayOfMonth() {
        Args args = Args.of(mNavRoute);
        if (args != null && args.mDayOfMonth != null) {
            return args.mDayOfMonth;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_ok) {
            int year = mDatePicker.getYear();
            int month = mDatePicker.getMonth();
            int day = mDatePicker.getDayOfMonth();
            getNavigator().pop(Result.newResult(year, month, day));
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

        private static Result newResult(int year, int month, int dayOfMonth) {
            Result result = new Result();
            result.mYear = year;
            result.mMonth = month;
            result.mDayOfMonth = dayOfMonth;
            return result;
        }

        private Integer mYear;
        private Integer mMonth;
        private Integer mDayOfMonth;

        public Integer getYear() {
            return mYear;
        }

        public Integer getMonth() {
            return mMonth;
        }

        public Integer getDayOfMonth() {
            return mDayOfMonth;
        }
    }

    public static class Args implements Serializable {
        public static Args newArgs(int year, int month, int day) {
            Args args = new Args();
            args.mYear = year;
            args.mMonth = month;
            args.mDayOfMonth = day;
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

        private Integer mYear;
        private Integer mMonth;
        private Integer mDayOfMonth;
    }
}
