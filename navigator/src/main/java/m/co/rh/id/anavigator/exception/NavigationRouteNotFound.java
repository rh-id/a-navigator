package m.co.rh.id.anavigator.exception;

/**
 * Exception thrown when navigation route not found
 */
public class NavigationRouteNotFound extends RuntimeException {
    public NavigationRouteNotFound(String message) {
        super(message);
    }
}
