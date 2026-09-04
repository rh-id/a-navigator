package m.co.rh.id.anavigator.r8smoke;

/**
 * Type of the component registered via
 * {@code NavConfiguration.Builder.setRequiredComponent(...)}.
 * A field of this interface type annotated with {@code @NavInject} must receive
 * the registered component instance through reflection.
 */
public interface ISmokeComponent {
    String getValue();
}
