package m.co.rh.id.anavigator.component;

import java.util.Objects;

/**
 * Library-owned mirror of the platform {@code android.window.BackEvent} that
 * describes an ongoing back gesture.
 * <p>
 * This class exists because a-navigator compiles against SDK 33 while
 * {@code android.window.BackEvent} and its related callbacks are API 34+ (and
 * the gesture events on the host side typically arrive through
 * {@code androidx.activity.OnBackAnimationCallback} which requires the
 * androidx.activity 1.8+ artifact). To keep the navigator module free of any
 * API 34 platform type AND free of any androidx.activity dependency, the
 * navigator defines its own immutable event carrier. Host apps translate the
 * platform/androidx event into this type when forwarding gesture progress
 * (see {@link INavigator#onBackStarted(NavBackEvent)}).
 * <p>
 * Instances are immutable and cheap, host apps may create one per gesture
 * frame.
 */
public class NavBackEvent {

    private final float mTouchX;
    private final float mTouchY;
    private final float mProgress;

    /**
     * @param touchX   X coordinate of the touch point, in pixels relative to the
     *                 receiving view/window
     * @param touchY   Y coordinate of the touch point, in pixels relative to the
     *                 receiving view/window
     * @param progress the gesture progress, a value between 0 (gesture started)
     *                 and 1 (gesture fully committed to the edge). Values are
     *                 clamped by the consumer, but callers should already
     *                 provide a value within the 0..1 range
     */
    public NavBackEvent(float touchX, float touchY, float progress) {
        mTouchX = touchX;
        mTouchY = touchY;
        mProgress = progress;
    }

    /**
     * @return the X coordinate of the touch point where the gesture started/is
     * ongoing, in pixels
     */
    public float getTouchX() {
        return mTouchX;
    }

    /**
     * @return the Y coordinate of the touch point where the gesture started/is
     * ongoing, in pixels
     */
    public float getTouchY() {
        return mTouchY;
    }

    /**
     * @return the gesture progress, between 0 (gesture just started) and
     * 1 (gesture reached the screen edge / commit threshold)
     */
    public float getProgress() {
        return mProgress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NavBackEvent)) return false;
        NavBackEvent that = (NavBackEvent) o;
        return Float.compare(that.mTouchX, mTouchX) == 0
                && Float.compare(that.mTouchY, mTouchY) == 0
                && Float.compare(that.mProgress, mProgress) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTouchX, mTouchY, mProgress);
    }

    @Override
    public String toString() {
        return "NavBackEvent{"
                + "touchX=" + mTouchX
                + ", touchY=" + mTouchY
                + ", progress=" + mProgress
                + '}';
    }
}
