package m.co.rh.id.anavigator.component;

import m.co.rh.id.anavigator.NavRoute;

/**
 * Listens for onRouteChanged event
 */
public interface NavOnRouteChangedListener {
    /**
     * @param previous previous route, may be null on first/initial route
     * @param current  current route to change to, may null when pop first/initial route
     */
    void onChanged(NavRoute previous, NavRoute current);
}
