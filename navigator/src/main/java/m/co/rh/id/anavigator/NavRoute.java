package m.co.rh.id.anavigator;

import java.io.Serializable;

import m.co.rh.id.anavigator.component.NavPopCallback;

@SuppressWarnings("rawtypes")
public class NavRoute implements Serializable {
    private NavPopCallback navPopCallback;
    private StatefulView statefulView;
    private final String routeName;
    private final Serializable routeArgs;
    private final String routeStateKey;
    private Serializable routeResult;

    NavRoute(NavPopCallback navPopCallback, StatefulView statefulView, String routeName, Serializable routeArgs, String routeStateKey) {
        this.navPopCallback = navPopCallback;
        this.statefulView = statefulView;
        this.routeName = routeName;
        this.routeArgs = routeArgs;
        this.routeStateKey = routeStateKey;
    }

    NavPopCallback getNavPopCallback() {
        return navPopCallback;
    }

    void setNavPopCallback(NavPopCallback navPopCallback) {
        this.navPopCallback = navPopCallback;
    }

    void setStatefulViewHandler(StatefulView statefulView) {
        this.statefulView = statefulView;
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
    public Object getRouteArgs() {
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
