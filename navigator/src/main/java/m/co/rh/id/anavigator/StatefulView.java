package m.co.rh.id.anavigator;

import android.app.Activity;
import android.view.View;

import java.util.UUID;

/**
 * An abstract object to act as a glue between state and view
 */
public abstract class StatefulView<ACT extends Activity> {
    private boolean mIsInitialized;
    private String mKey;

    public StatefulView() {
        this(null);
    }

    /**
     * @param key Unique key for this stateful object,
     *            to be use as a key to save the state,
     *            if null default to built in unique key using class name, may be null
     */
    public StatefulView(String key) {
        mKey = key == null ? this.getClass().getName() + "-StatefulViewClassKey-" +
                UUID.randomUUID().toString()
                : key;
    }

    /**
     * Unique key for this stateful object, to be use as a key to save the state.
     *
     * @return non-null key
     */
    public String getKey() {
        return mKey;
    }

    /**
     * @return true if this has been initialized, false otherwise
     */
    public final boolean isInitialized() {
        return mIsInitialized;
    }

    /**
     * Dispose this and will not be used anymore.
     */
    public void dispose(ACT activity) {
        // nothing to dispose on parent here
    }

    /**
     * Build new view instance and return it,
     */
    public final View buildView(ACT activity) {
        initialize(activity);
        return createView(activity);
    }

    /**
     * Initialize state. This method will be called once only when {@link #buildView(Activity)} is called
     */
    protected void initState(ACT activity) {
        mIsInitialized = true;
    }

    /**
     * Create new view
     */
    protected abstract View createView(ACT activity);

    private void initialize(ACT activity) {
        if (!mIsInitialized) {
            mIsInitialized = true;
            initState(activity);
        }
    }
}
