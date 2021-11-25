package m.co.rh.id.anavigator;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import java.io.Serializable;
import java.util.UUID;

/**
 * An abstract object to act as a glue between state and view.
 * NOTE: Always call super implementation when extending this.
 */
public abstract class StatefulView<ACT extends Activity> implements Serializable {
    private boolean mIsInitialized;
    private final String mKey;

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
     *
     * @return view from {@link #createView(Activity, ViewGroup)}
     */
    public final View buildView(ACT activity, ViewGroup container) {
        initialize(activity);
        return createView(activity, container);
    }

    /**
     * Initialize state. This method will be called once only when {@link #buildView(Activity, ViewGroup)} is called.
     * Don't call this method directly, call {@link #buildView(Activity, ViewGroup)} instead
     */
    protected void initState(ACT activity) {
        mIsInitialized = true;
    }

    /**
     * Create new view, don't call this method directly, call {@link #buildView(Activity, ViewGroup)} instead.
     */
    protected abstract View createView(ACT activity, ViewGroup container);

    /**
     * Initialize this stateful view
     */
    public final void initialize(ACT activity) {
        if (!mIsInitialized) {
            mIsInitialized = true;
            initState(activity);
        }
    }
}
