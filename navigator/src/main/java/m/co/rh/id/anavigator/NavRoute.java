package m.co.rh.id.anavigator;

import m.co.rh.id.anavigator.component.NavPopCallback;

public class NavRoute {
    private NavPopCallback navPopCallback;
    private StatefulView statefulView;
    private final String routeName;
    private final Object routeArgs;
    private final String routeStateKey;
    private Object routeResult;

    NavRoute(NavPopCallback navPopCallback, StatefulView statefulView, String routeName, Object routeArgs, String routeStateKey) {
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

    void setRouteResult(Object routeResult) {
        this.routeResult = routeResult;
    }
}
