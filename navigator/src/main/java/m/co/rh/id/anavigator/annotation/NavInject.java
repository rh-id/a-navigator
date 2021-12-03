package m.co.rh.id.anavigator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates field to be injected by Navigator.
 * If StatefulView field is annotated with this and it is NOT NULL,
 * then attempt to inject its field, similar to using INavigator.injectRequired
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NavInject {
}
