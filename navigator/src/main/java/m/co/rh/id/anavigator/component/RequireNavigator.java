package m.co.rh.id.anavigator.component;

import m.co.rh.id.anavigator.NavRoute;

/**
 * implement this on StatefulView if you need to inject current Navigator that handle current instance.
 */
public interface RequireNavigator {
    /**
     * Inject navigator instance, this is called before initState and only once.
     * NOTE : Special case IF save state is enabled.
     * this will be called again once when StatefulView was restored from state.
     * Reason? Navigator can't be serialized or saved,
     * so when StatefulView was restored, any INavigator reference will be null.
     *
     * @param navigator that handles current StatefulView instance
     * @param navRoute  that is associated with current StatefulView
     */
    void provideNavigator(INavigator navigator, NavRoute navRoute);
}
