package m.co.rh.id.anavigator.component;

import android.content.Intent;

import java.io.Serializable;

import m.co.rh.id.anavigator.NavConfiguration;
import m.co.rh.id.anavigator.NavRoute;
import m.co.rh.id.anavigator.exception.NavigationRouteNotFound;

public interface INavigator {
    /**
     * Navigate to anonymous route using StatefulViewFactory.
     *
     * @param statefulViewFactory must not null
     * @param args                arguments to be passed to this route, nullable
     * @param navPopCallback      callback to handle when this route pop/exit
     */
    void push(StatefulViewFactory statefulViewFactory, Serializable args, NavPopCallback navPopCallback);

    /**
     * Navigate to route using route name.
     *
     * @param routeName      route name, must not null
     * @param args           arguments to be passed to this route, nullable
     * @param navPopCallback callback to handle when this route pop/exit
     * @throws NavigationRouteNotFound if route not found
     */
    void push(String routeName, Serializable args, NavPopCallback navPopCallback);

    /**
     * Same as {@link #push(String, Serializable, NavPopCallback)}
     * with NavPopCallback value null
     */
    void push(String routeName, Serializable args);

    /**
     * Same as {@link #push(String, Serializable, NavPopCallback)}
     * with args null and NavPopCallback value null
     */
    void push(String routeName);

    /**
     * Return to previous destination with result.
     * If current route is initial route then calling this will finish the activity.
     * If the result is NOT null activity will finish with result Activity.RESULT_OK,
     * otherwise Activity.RESULT_CANCELED.
     * The result will only be set to intent result only if it is Serializable type OR Intent type.
     * If Serializable, the result will be set as activity result using navigator defined key
     * If Intent, result will be set directly as result to activity.
     *
     * @param result result to pass to the callback specified by NavPopCallback
     *               when push {@link #push(String, Serializable, NavPopCallback)} to current destination
     * @return true if there are route to pop, false otherwise.
     */
    boolean pop(Serializable result);

    /**
     * Same as {@link #pop(Serializable)} with null result
     */
    boolean pop();

    /**
     * Finish activity with result.
     * The result will only be set to intent result only if it is Serializable type OR Intent type.
     * If Serializable, the result will be set as activity result using navigator defined key
     * If Intent, result will be set directly as result to activity
     */
    void finishActivity(Object result);

    /**
     * @return this navigator nav configuration, for action bar purposes perhaps
     */
    NavConfiguration getNavConfiguration();

    /**
     * @return current navigator route
     */
    NavRoute getCurrentRoute();

    /**
     * Signal to navigator that back button is pressed,
     * let the currently displayed view handle back button else {@link #pop()}
     */
    void onBackPressed();

    /**
     * Signal to navigator that activity result is invoked,
     * let the currently displayed view handle this result
     */
    void onActivityResult(int requestCode, int resultCode, Intent data);

    /**
     * is current navigator at the initial route?
     */
    boolean isInitialRoute();
}
