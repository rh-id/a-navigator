package m.co.rh.id.anavigator.component;

/**
 * implement this on StatefulView if you need to inject current Navigator that handle current instance.
 */
public interface RequireNavigator {
    /**
     * Inject navigator instance, this is called before initState and only once
     *
     * @param navigator that handles current StatefulView instance
     */
    void provideNavigator(INavigator navigator);
}
