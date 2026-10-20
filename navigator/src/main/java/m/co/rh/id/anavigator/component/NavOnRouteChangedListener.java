package m.co.rh.id.anavigator.component;

import m.co.rh.id.anavigator.NavRoute;

/**
 * Listens for onRouteChanged event.
 * The event fires after every push/pop AND exactly once after a saved-state
 * restore, where {@code previous} is null and {@code current} is the restored
 * top route.
 */
public interface NavOnRouteChangedListener {
    /**
     * @param previous previous route, may be null on first/initial route
     *                 or when announcing a state restore
     * @param current  current route to change to, may null when pop first/initial route
     */
    void onChanged(NavRoute previous, NavRoute current);
}
