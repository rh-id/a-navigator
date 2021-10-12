package m.co.rh.id.anavigator;

import java.io.Serializable;

import m.co.rh.id.anavigator.component.NavPopCallback;
import m.co.rh.id.anavigator.component.StatefulViewFactory;

@SuppressWarnings("rawtypes")
public class NavRoute implements Serializable {
    private final StatefulViewFactory statefulViewFactory;
    private final NavPopCallback navPopCallback;
    private final RouteOptions routeOptions;
    private final StatefulView statefulView;
    private final String routeName;
    private final Serializable routeArgs;
    private final String routeStateKey;
    private Serializable routeResult;

    NavRoute(StatefulViewFactory statefulViewFactory,
             NavPopCallback navPopCallback,
             RouteOptions routeOptions,
             StatefulView statefulView,
             String routeName, Serializable routeArgs, String routeStateKey) {
        this.statefulViewFactory = statefulViewFactory;
        this.navPopCallback = navPopCallback;
        this.routeOptions = routeOptions;
        this.statefulView = statefulView;
        this.routeName = routeName;
        this.routeArgs = routeArgs;
        this.routeStateKey = routeStateKey;
    }

    StatefulViewFactory getStatefulViewFactory() {
        return statefulViewFactory;
    }

    NavPopCallback getNavPopCallback() {
        return navPopCallback;
    }

    RouteOptions getRouteOptions() {
        return routeOptions;
    }

    StatefulView getStatefulView() {
        return statefulView;
    }

    /**
     * route name, might be null if this is anonymous route.
     * a route name that is not defined in navigation map
     */
    public String getRouteName() {
        return routeName;
    }

    /**
     * Arguments passed for this route
     */
    public Serializable getRouteArgs() {
        return routeArgs;
    }

    /**
     * Key for route state
     */
    public String getRouteStateKey() {
        return routeStateKey;
    }

    /**
     * Current route result, can be null if no result or not yet set
     */
    public Object getRouteResult() {
        return routeResult;
    }

    void setRouteResult(Serializable routeResult) {
        this.routeResult = routeResult;
    }
}
