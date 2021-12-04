package m.co.rh.id.anavigator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates field to be injected by Navigator.
 * Inject current stateful view with route index. Same as using INavigator.findRouteIndex(navRouteOfTheStatefulView)
 * Apply this to Number type or its primitives (Byte,Short,Integer,Long,Float,Double)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NavRouteIndex {
}
