package m.co.rh.id.anavigator.component;

import m.co.rh.id.anavigator.NavRoute;

/**
 * implement this on StatefulView if you need to inject current NavRoute associated with StatefulView instance
 */
public interface RequireNavRoute {
    /**
     * Inject navRoute instance, this is called before initState and only once.
     * NOTE : Special case IF save state is enabled.
     * this will be called again once when StatefulView was restored from state.
     *
     * @param navRoute that associated with current StatefulView instance
     */
    void provideNavRoute(NavRoute navRoute);
}
