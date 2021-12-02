package m.co.rh.id.anavigator.component;

/**
 * implement this on StatefulView if you need to inject your own component configured by NavConfiguration
 */
public interface RequireComponent<COMPONENT> {
    /**
     * Inject component instance, this is called before initState and only once.
     * NOTE : Special case IF save state is enabled.
     * this will be called again once when StatefulView was restored from state.
     *
     * @param component configured on NavConfiguration
     */
    void provideComponent(COMPONENT component);
}
