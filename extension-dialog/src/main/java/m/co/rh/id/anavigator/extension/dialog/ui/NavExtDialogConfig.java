package m.co.rh.id.anavigator.extension.dialog.ui;

import android.app.Activity;
import android.content.Context;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.component.StatefulViewFactory;
import m.co.rh.id.anavigator.extension.dialog.R;

public class NavExtDialogConfig {
    /**
     * Route to dialog with title, content body, and ok button to show message only
     */
    public static final String ROUTE_MESSAGE = "ROUTE_MESSAGE";

    // mapping of route constant with resource string
    private Map<String, String> mConstantRouteMap;

    private Map<String, StatefulViewFactory<Activity, StatefulView<Activity>>> mNavMap;

    public NavExtDialogConfig(Context context) {
        String routeMessage = context.getString(R.string.a_navigator_extension_dialog_route_message);
        mConstantRouteMap = new LinkedHashMap<>();
        mConstantRouteMap.put(ROUTE_MESSAGE, routeMessage);
        mNavMap = new LinkedHashMap<>();
        mNavMap.put(routeMessage, (args, activity) -> new MessageSVDialog());
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
}
