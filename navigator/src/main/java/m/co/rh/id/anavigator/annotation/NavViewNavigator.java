package m.co.rh.id.anavigator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates field to be injected by Navigator.
 * Inject current stateful view with ViewNavigator. Same as using INavigator.findViewNavigator(viewNavigatorId).
 * Apply this to INavigator type
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NavViewNavigator {
    /**
     * ViewGroup container ID name to be used to find ViewNavigator.
     * example:
     * if ID is R.id.my_unique_view_nav_container set string value "my_unique_view_nav_container"
     */
    String value();
}
