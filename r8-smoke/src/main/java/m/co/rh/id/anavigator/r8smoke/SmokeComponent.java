package m.co.rh.id.anavigator.r8smoke;

public class SmokeComponent implements ISmokeComponent {

    public static final String EXPECTED_VALUE = "smoke-component-value";

    @Override
    public String getValue() {
        return EXPECTED_VALUE;
    }
}
