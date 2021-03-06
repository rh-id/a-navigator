package m.co.rh.id.anavigator.extension.dialog.ui;

import android.app.Activity;
import android.content.Context;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import m.co.rh.id.anavigator.NavRoute;
import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.StatefulViewFactory;
import m.co.rh.id.anavigator.extension.dialog.R;

public class NavExtDialogConfig {
    private static final String ROUTE_MESSAGE = "ROUTE_MESSAGE";
    private static final String ROUTE_CONFIRM = "ROUTE_CONFIRM";
    private static final String ROUTE_DATE_TIME_PICKER = "ROUTE_DATE_TIME_PICKER";
    private static final String ROUTE_TIME_PICKER = "ROUTE_TIME_PICKER";
    private static final String ROUTE_DATE_PICKER = "ROUTE_DATE_PICKER";

    // mapping of route constant with resource string
    private Map<String, String> mConstantRouteMap;

    private Map<String, StatefulViewFactory<Activity, StatefulView<Activity>>> mNavMap;

    public NavExtDialogConfig(Context context) {
        String routeMessage = context.getString(R.string.a_navigator_extension_dialog_route_message);
        String routeConfirm = context.getString(R.string.a_navigator_extension_dialog_route_confirm);
        String routeDateTimePicker = context.getString(R.string.a_navigator_extension_dialog_route_date_time_picker);
        String routeTimePicker = context.getString(R.string.a_navigator_extension_dialog_route_time_picker);
        String routeDatePicker = context.getString(R.string.a_navigator_extension_dialog_route_date_picker);
        mConstantRouteMap = new LinkedHashMap<>();
        mConstantRouteMap.put(ROUTE_MESSAGE, routeMessage);
        mConstantRouteMap.put(ROUTE_CONFIRM, routeConfirm);
        mConstantRouteMap.put(ROUTE_DATE_TIME_PICKER, routeDateTimePicker);
        mConstantRouteMap.put(ROUTE_TIME_PICKER, routeTimePicker);
        mConstantRouteMap.put(ROUTE_DATE_PICKER, routeDatePicker);
        mNavMap = new LinkedHashMap<>();
        mNavMap.put(routeMessage, (args, activity) -> new MessageSVDialog());
        mNavMap.put(routeConfirm, (args, activity) -> new ConfirmSVDialog());
        mNavMap.put(routeDateTimePicker, (args, activity) -> new DateTimePickerSVDialog());
        mNavMap.put(routeTimePicker, (args, activity) -> new TimePickerSVDialog());
        mNavMap.put(routeDatePicker, (args, activity) -> new DatePickerSVDialog());
    }

    /**
     * @return NavMap for this module
     */
    public Map<String, StatefulViewFactory<Activity, StatefulView<Activity>>> getNavMap() {
        return mNavMap;
    }

    /**
     * Route to dialog with title, content body, and ok button to show message only
     */
    public String route_messageDialog() {
        return mConstantRouteMap.get(ROUTE_MESSAGE);
    }

    /**
     * Route to dialog with title, content body, cancel and ok button to show.
     * If the dialog is canceled by clicking cancel button, it will return false.
     * if dismissed outside or back button dismiss, it will return null,
     * If ok button is clicked, it will return true
     */
    public String route_confirmDialog() {
        return mConstantRouteMap.get(ROUTE_CONFIRM);
    }

    /**
     * Route to dialog with date picker and time picker
     */
    public String route_dateTimePickerDialog() {
        return mConstantRouteMap.get(ROUTE_DATE_TIME_PICKER);
    }

    /**
     * Route to dialog with time picker
     */
    public String route_timePickerDialog() {
        return mConstantRouteMap.get(ROUTE_TIME_PICKER);
    }

    /**
     * Route to dialog with date picker
     */
    public String route_datePickerDialog() {
        return mConstantRouteMap.get(ROUTE_DATE_PICKER);
    }

    /**
     * Preparing arguments for message dialog ({@link #ROUTE_MESSAGE})
     *
     * @param title   title to be shown
     * @param message message body/content
     * @return arguments, pass this as argument to the navigator
     */
    public Serializable args_messageDialog(String title, String message) {
        return MessageSVDialog.Args.newArgs(title, message);
    }

    /**
     * Preparing arguments for confirm dialog ({@link #ROUTE_CONFIRM})
     *
     * @param title   title to be shown
     * @param message message body/content
     * @return arguments, pass this as argument to the navigator
     */
    public Serializable args_confirmDialog(String title, String message) {
        return ConfirmSVDialog.Args.newArgs(title, message);
    }

    /**
     * get the result of the confirm dialog
     *
     * @param navRoute the navRoute of the confirm dialog when popped
     * @return true if OK button is pressed, false if CANCEL button is pressed,
     * and null if dismissed without pressing any button OR
     * if nav route doesn't actually have any of the result (perhaps incorrect navRoute instance)
     */
    public Boolean result_confirmDialog(NavRoute navRoute) {
        ConfirmSVDialog.Result result = ConfirmSVDialog.Result.of(navRoute);
        if (result == null) return null;
        return result.isConfirmed();
    }

    /**
     * Preparing arguments for Date time picker dialog ({@link #ROUTE_DATE_TIME_PICKER})
     *
     * @param is24HourFormat whether the time picker show 24 hour format or not, set to true if null
     * @param date           date to be set for this picker, set to current date if null
     * @return arguments, pass this as argument to the navigator
     */
    public Serializable args_dateTimePickerDialog(Boolean is24HourFormat, Date date) {
        return args_dateTimePickerDialog(
                is24HourFormat, date, null, null);
    }

    /**
     * Preparing arguments for Date time picker dialog ({@link #ROUTE_DATE_TIME_PICKER})
     *
     * @param is24HourFormat whether the time picker show 24 hour format or not, set to true if null
     * @param date           date to be set for this picker, set to current date if null
     * @param minDate        min date to be set, can be null
     * @param maxDate        max date to be set, can be null
     * @return arguments, pass this as argument to the navigator
     */
    public Serializable args_dateTimePickerDialog(Boolean is24HourFormat, Date date, Date minDate, Date maxDate) {
        return DateTimePickerSVDialog.Args.newArgs(is24HourFormat, date, minDate, maxDate);
    }

    /**
     * get the result of the date time picker dialog
     *
     * @param navRoute the navRoute of the confirm dialog when popped
     * @return selected date time or null if navRoute doesn't contain result
     */
    public Date result_dateTimePickerDialog(NavRoute navRoute) {
        DateTimePickerSVDialog.Result result = DateTimePickerSVDialog.Result.of(navRoute);
        if (result == null) return null;
        return result.getDate();
    }

    /**
     * Preparing arguments for Time picker dialog ({@link #ROUTE_TIME_PICKER})
     *
     * @param is24HourFormat whether the time picker show 24 hour format or not, set to true if null
     * @param hourOfDay      hour of day to be set, set to current hour if null
     * @param minute         minute to be set, set to current minute if null
     * @return arguments, pass this as argument to the navigator
     */
    public Serializable args_timePickerDialog(Boolean is24HourFormat, Integer hourOfDay, Integer minute) {
        return TimePickerSVDialog.Args.newArgs(is24HourFormat, hourOfDay, minute);
    }

    /**
     * get the result of the time picker dialog
     *
     * @param navRoute the navRoute of the confirm dialog when popped
     * @return selected hour of day or null if navRoute doesn't contain result
     */
    public Integer result_timePickerDialog_hourOfDay(NavRoute navRoute) {
        TimePickerSVDialog.Result result = TimePickerSVDialog.Result.of(navRoute);
        if (result == null) return null;
        return result.getHourOfDay();
    }

    /**
     * get the result of the time picker dialog
     *
     * @param navRoute the navRoute of the confirm dialog when popped
     * @return selected minute or null if navRoute doesn't contain result
     */
    public Integer result_timePickerDialog_minute(NavRoute navRoute) {
        TimePickerSVDialog.Result result = TimePickerSVDialog.Result.of(navRoute);
        if (result == null) return null;
        return result.getMinute();
    }

    /**
     * Preparing arguments for Date picker dialog ({@link #ROUTE_DATE_PICKER})
     *
     * @param year       set the year
     * @param month      set the month
     * @param dayOfMonth set the dayOfMonth
     * @return arguments, pass this as argument to the navigator
     */
    public Serializable args_datePickerDialog(int year, int month, int dayOfMonth) {
        return args_datePickerDialog(year, month, dayOfMonth, null, null);
    }

    /**
     * Preparing arguments for Date picker dialog ({@link #ROUTE_DATE_PICKER})
     *
     * @param year       set the year
     * @param month      set the month
     * @param dayOfMonth set the dayOfMonth
     * @param minDate    set min date, can be null
     * @param maxDate    set max date, can be null
     * @return arguments, pass this as argument to the navigator
     */
    public Serializable args_datePickerDialog(int year, int month, int dayOfMonth, Date minDate, Date maxDate) {
        return DatePickerSVDialog.Args.newArgs(year, month, dayOfMonth, minDate, maxDate);
    }

    /**
     * Preparing arguments for Date picker dialog ({@link #ROUTE_DATE_PICKER})
     *
     * @param date to extract year, month, and dayOfMonth based on default calendar instance
     * @return arguments, pass this as argument to the navigator
     */
    public Serializable args_datePickerDialog(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return args_datePickerDialog(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Preparing arguments for Date picker dialog ({@link #ROUTE_DATE_PICKER})
     *
     * @param date    to extract year, month, and dayOfMonth based on default calendar instance
     * @param minDate set min date, can be null
     * @param maxDate set max date, can be null
     * @return arguments, pass this as argument to the navigator
     */
    public Serializable args_datePickerDialog(Date date, Date minDate, Date maxDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return args_datePickerDialog(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH), minDate, maxDate);
    }

    /**
     * get the result of the date picker dialog
     *
     * @param navRoute the navRoute of the confirm dialog when popped
     * @return selected year or null if navRoute doesn't contain result
     */
    public Integer result_datePickerDialog_year(NavRoute navRoute) {
        DatePickerSVDialog.Result result = DatePickerSVDialog.Result.of(navRoute);
        if (result == null) return null;
        return result.getYear();
    }

    /**
     * get the result of the date picker dialog
     *
     * @param navRoute the navRoute of the confirm dialog when popped
     * @return selected month or null if navRoute doesn't contain result
     */
    public Integer result_datePickerDialog_month(NavRoute navRoute) {
        DatePickerSVDialog.Result result = DatePickerSVDialog.Result.of(navRoute);
        if (result == null) return null;
        return result.getMonth();
    }

    /**
     * get the result of the date picker dialog
     *
     * @param navRoute the navRoute of the confirm dialog when popped
     * @return selected dayOfMonth or null if navRoute doesn't contain result
     */
    public Integer result_datePickerDialog_dayOfMonth(NavRoute navRoute) {
        DatePickerSVDialog.Result result = DatePickerSVDialog.Result.of(navRoute);
        if (result == null) return null;
        return result.getDayOfMonth();
    }
}
