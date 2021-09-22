package m.co.rh.id.anavigator.component;

/**
 * Callback when navigator pops
 */
public interface NavPopCallback<RESULT> {
    void onPop(RESULT result);
}
