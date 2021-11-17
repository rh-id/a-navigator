package m.co.rh.id.anavigator;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import m.co.rh.id.anavigator.component.NavPopCallback;
import m.co.rh.id.anavigator.component.StatefulViewFactory;

@SuppressWarnings("rawtypes")
public class NavRoute implements Externalizable {
    private StatefulViewFactory statefulViewFactory;
    private NavPopCallback navPopCallback;
    private RouteOptions routeOptions;
    private StatefulView statefulView;
    private String routeName;
    private Serializable routeArgs;
    private String routeStateKey;
    private Serializable routeResult;

    /**
     * Do not use this in production.
     * This constructor is meant for serialization purpose only.
     */
    public NavRoute() {
        // leave blank
    }

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

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(statefulViewFactory);
        out.writeObject(navPopCallback);
        out.writeObject(routeOptions);
        out.writeObject(statefulView);
        out.writeObject(routeName);
        out.writeObject(routeArgs);
        out.writeObject(routeStateKey);
        out.writeObject(routeResult);
    }

    @Override
    public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
        statefulViewFactory = (StatefulViewFactory) in.readObject();
        navPopCallback = (NavPopCallback) in.readObject();
        routeOptions = (RouteOptions) in.readObject();
        statefulView = (StatefulView) in.readObject();
        routeName = (String) in.readObject();
        routeArgs = (Serializable) in.readObject();
        routeStateKey = (String) in.readObject();
        routeResult = (Serializable) in.readObject();
    }
}
