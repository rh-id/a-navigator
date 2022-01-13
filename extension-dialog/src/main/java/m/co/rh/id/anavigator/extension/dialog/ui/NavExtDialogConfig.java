package m.co.rh.id.anavigator.extension.dialog.ui;

import android.app.Activity;
import android.content.Context;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import m.co.rh.id.anavigator.NavRoute;
import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.StatefulViewFactory;
import m.co.rh.id.anavigator.extension.dialog.R;

public class NavExtDialogConfig {
    /**
     * Route to dialog with title, content body, and ok button to show message only
     */
    public static final String ROUTE_MESSAGE = "ROUTE_MESSAGE";
    /**
     * Route to dialog with title, content body, cancel and ok button to show.
     * If the dialog is canceled by clicking cancel button, it will return false.
     * if dismissed outside or back button dismiss, it will return null,
     * If ok button is clicked, it will return true
     */
    public static final String ROUTE_CONFIRM = "ROUTE_CONFIRM";

    // mapping of route constant with resource string
    private Map<String, String> mConstantRouteMap;

    private Map<String, StatefulViewFactory<Activity, StatefulView<Activity>>> mNavMap;

    public NavExtDialogConfig(Context context) {
        String routeMessage = context.getString(R.string.a_navigator_extension_dialog_route_message);
        String routeConfirm = context.getString(R.string.a_navigator_extension_dialog_route_confirm);
        mConstantRouteMap = new LinkedHashMap<>();
        mConstantRouteMap.put(ROUTE_MESSAGE, routeMessage);
        mConstantRouteMap.put(ROUTE_CONFIRM, routeConfirm);
        mNavMap = new LinkedHashMap<>();
        mNavMap.put(routeMessage, (args, activity) -> new MessageSVDialog());
        mNavMap.put(routeConfirm, (args, activity) -> new ConfirmSVDialog());
    }

    /**
     * @return NavMap for this module
     */
    public Map<String, StatefulViewFactory<Activity, StatefulView<Activity>>> getNavMap() {
        return mNavMap;
    }

    /**
     * Get route path for routing.
     * use this route to push to the navigator.
     * OR use context.getString to get actual route path and push to navigator
     *
     * @param route ex: {@link #ROUTE_MESSAGE}
     */
    public String getRoutePath(String route) {
        return mConstantRouteMap.get(route);
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

}
