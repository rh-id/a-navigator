package m.co.rh.id.anavigator.component;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import java.io.Serializable;
import java.util.regex.Pattern;

import m.co.rh.id.anavigator.NavConfiguration;
import m.co.rh.id.anavigator.NavRoute;
import m.co.rh.id.anavigator.RouteOptions;
import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.exception.NavigationRouteNotFound;

public interface INavigator {
    /**
     * Same as {@link #push(StatefulViewFactory, Serializable, NavPopCallback)}
     * with Serializable and NavPopCallback value null
     */
    void push(StatefulViewFactory statefulViewFactory);

    /**
     * Same as {@link #push(StatefulViewFactory, Serializable, NavPopCallback)}
     * with NavPopCallback value null
     */
    void push(StatefulViewFactory statefulViewFactory, Serializable args);

    /**
     * Same as {@link #push(StatefulViewFactory, Serializable, NavPopCallback)}
     * with args value null
     */
    void push(StatefulViewFactory statefulViewFactory, NavPopCallback navPopCallback);

    /**
     * Same as {@link #push(StatefulViewFactory, Serializable, NavPopCallback, RouteOptions)}
     * with RouteOptions value null
     *
     * @param statefulViewFactory must not null
     * @param args                arguments to be passed to this route, nullable
     * @param navPopCallback      callback to handle when this route pop/exit
     */
    void push(StatefulViewFactory statefulViewFactory, Serializable args, NavPopCallback navPopCallback);

    /**
     * Navigate to anonymous route using StatefulViewFactory.
     *
     * @param statefulViewFactory must not null
     * @param args                arguments to be passed to this route, nullable
     * @param navPopCallback      callback to handle when this route pop/exit
     * @param routeOptions        routeOptions to use for this route
     */
    void push(StatefulViewFactory statefulViewFactory, Serializable args, NavPopCallback navPopCallback, RouteOptions routeOptions);

    /**
     * Navigate to route using route name.
     *
     * @param routeName      route name, must not null
     * @param args           arguments to be passed to this route, nullable
     * @param navPopCallback callback to handle when this route pop/exit
     * @param routeOptions   routeOptions to use for this route
     * @throws NavigationRouteNotFound if route not found
     */
    void push(String routeName, Serializable args, NavPopCallback navPopCallback, RouteOptions routeOptions);

    /**
     * Same as {@link #push(String, Serializable, NavPopCallback, RouteOptions)}
     * with RouteOptions value null
     */
    void push(String routeName, Serializable args, NavPopCallback navPopCallback);

    /**
     * Same as {@link #push(String, Serializable, NavPopCallback, RouteOptions)}
     * with args value null and RouteOptions value null
     */
    void push(String routeName, NavPopCallback navPopCallback);

    /**
     * Same as {@link #push(String, Serializable, NavPopCallback, RouteOptions)}
     * with NavPopCallback value null and RouteOptions value null
     */
    void push(String routeName, Serializable args);

    /**
     * Same as {@link #push(String, Serializable, NavPopCallback, RouteOptions)}
     * with args value null, NavPopCallback value null, and RouteOptions value null
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
     *               when push {@link #push(String, Serializable, NavPopCallback, RouteOptions)} to current destination
     * @return true if there are route to pop, false otherwise.
     */
    boolean pop(Serializable result);

    /**
     * Same as {@link #pop(Serializable)} with null result
     */
    boolean pop();

    /**
     * Return/pop to previous until specified route name is reached.
     *
     * @param routeName route name to pop until
     * @param result    result to be returned and forwarded to each NavPopCallback of the route it pops
     * @throws NavigationRouteNotFound if routeName not found
     */
    void popUntil(String routeName, Serializable result);

    /**
     * Same as {@link #popUntil(String, Serializable)} with null result
     */
    void popUntil(String routeName);

    /**
     * Same as {@link #retry()} but override original args with supplied args
     */
    void retry(Serializable overrideArgs);

    /**
     * Retry the same route, basically means pop current route without triggering pop callback
     * and push the same route with the same args and callback
     */
    void retry();

    /**
     * Rebuild a route specified by routeIndex.
     * Basically this will call StatefulView.createView without disposing or popping the route.
     *
     * @param routeIndex route index to rebuild,
     *                   initial route index is 0, when navigator is pushed index increase by 1,
     *                   when popped decrease by 1
     * @throws NavigationRouteNotFound if route specified by routeIndex not found
     */
    void reBuildRoute(int routeIndex);

    /**
     * Rebuild a route where StatefulView.key string contains the pattern.
     *
     * @param pattern to be matched with StatefulView.key
     */
    void reBuildRoute(Pattern pattern);

    /**
     * Rebuild all route, start from initial route.
     */
    void reBuildAllRoute();

    /**
     * Finish activity with result.
     * The result will only be set to intent result only if it is Serializable type OR Intent type.
     * If Serializable, the result will be set as activity result using navigator defined key
     * If Intent, result will be set directly as result to activity
     */
    void finishActivity(Object result);

    /**
     * Finish activity without result.
     */
    void finishActivity();

    /**
     * Add callback listener to listen to changing route pop and push.
     * The callback will be triggered after pop or push had happened
     */
    void addOnRouteChangedListener(NavOnRouteChangedListener navOnRouteChangedListener);

    /**
     * Remove the OnRouteChangedListener callback
     */
    void removeOnRouteChangedListener(NavOnRouteChangedListener navOnRouteChangedListener);

    /**
     * Create and register view navigator.
     * Based on supplied "viewGroupContainerId", navigator will be initialized when it found this ID.
     * Due to Activity lifecycle dependency,
     * You need to setup and call this during Application creation
     * <p>
     * NOTE: The resulting navigator will not receive
     * onBackPressed and onActivityResult event.
     * <p>
     * Pop behaviour will be different,
     * if ViewNavigator reaches initial route, it won't finish activity
     *
     * @param navConfiguration     nav configuration for this navigator
     * @param viewGroupContainerId container ID for this navigator,
     *                             make sure its ID is unique across whole app
     */
    void createViewNavigator(NavConfiguration navConfiguration, int viewGroupContainerId);

    /**
     * Find view navigator registered using {@link #createViewNavigator(NavConfiguration, int)}
     *
     * @param viewGroupContainerId unique container ID that was registered
     */
    INavigator findViewNavigator(int viewGroupContainerId);

    /**
     * Inject StatefulView with required components from the navigator
     * <p>
     * NOTE:
     * RequireNavRoute will be injected based on #parentStatefulView NavRoute.
     * If #parentStatefulView not found in Navigator, NavRoute will be null
     *
     * @param parentStatefulView parent StatefulView that #statefulViews resides
     * @param statefulViews      that implements RequireComponent, RequireNavigator, RequireNavRoute
     */
    void injectRequired(StatefulView parentStatefulView, StatefulView... statefulViews);

    /**
     * @param navRoute to be searched in navigator
     * @return route index found using navRoute param. -1 if not found
     * <p>
     * initial route index is 0 (your home page),
     * when navigator is pushed index increase by 1,
     * when popped decrease by 1
     */
    int findRouteIndex(NavRoute navRoute);

    /**
     * @param navRoute to be searched
     * @return View for the NavRoute or null if not found,
     * If the NavRoute points to StatefulViewDialog it will also return null,
     * if NavRoute is valid from this navigator then it WILL return value
     */
    View findView(NavRoute navRoute);

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
     * let the currently displayed StatefulView handle this result
     */
    void onActivityResult(int requestCode, int resultCode, Intent data);

    /**
     * Signal to navigator that activity request permission result is invoked,
     * let the currently displayed StatefulView handle this result
     */
    void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

    /**
     * is current navigator at the initial route?
     */
    boolean isInitialRoute();

    /**
     * @return Activity instance for this navigator.
     * Maybe null if activity hasn't been created yet
     */
    Activity getActivity();
}
