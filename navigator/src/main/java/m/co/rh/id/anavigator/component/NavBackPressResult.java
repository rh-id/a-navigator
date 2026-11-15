package m.co.rh.id.anavigator.component;

import java.io.Serializable;

/**
 * Represents the back-press decision of a route (StatefulView) that
 * implements {@link NavOnBackPressed}.
 * <p>
 * When the navigator forwards a back press to the current top route, the
 * route returns an instance of this class to declare its outcome:
 * <ul>
 *     <li>{@link #pop()} / {@link #pop(Serializable)} - the navigator pops
 *     the top route; when a result is provided it is delivered to the
 *     pop callback of the popped route (same mechanism as
 *     {@link INavigator#pop(Serializable)})</li>
 *     <li>{@link #noPop()} - the route handled the back press itself (e.g.
 *     closed a drawer, showed a confirmation) and the route stack must be
 *     kept unchanged</li>
 * </ul>
 * The framework owns the pop: a route that wants to pop on back should
 * return a pop decision instead of calling {@code navigator.pop()} itself,
 * so the navigator performs the pop with the correct transition handling.
 * <p>
 * Contract details:
 * <ul>
 *     <li>a non-null return value is followed by the navigator exactly as
 *     decided - the carried result is honored only when
 *     {@link #shouldPop()} is true</li>
 *     <li>a <b>null</b> return value means the route has no opinion and the
 *     navigator performs its default behavior (pop), identical to a route
 *     that does not implement {@link NavOnBackPressed} at all. The only way
 *     to swallow the back press is an explicit {@link #noPop()}</li>
 * </ul>
 * Instances are immutable and cheap, routes may return a shared constant if
 * desired.
 *
 * @see NavOnBackPressed#onBackPressed(View, Activity, INavigator)
 */
public class NavBackPressResult implements Serializable {

    private final boolean mShouldPop;
    private final Serializable mResult;

    private NavBackPressResult(boolean shouldPop, Serializable result) {
        mShouldPop = shouldPop;
        mResult = result;
    }

    /**
     * Decision to pop the current route without any result.
     *
     * @return a back-press decision that pops the top route
     */
    public static NavBackPressResult pop() {
        return new NavBackPressResult(true, null);
    }

    /**
     * Decision to pop the current route and deliver the provided result to
     * the pop callback of the popped route.
     *
     * @param result result to deliver with the pop, may be null (equivalent
     *               to {@link #pop()})
     * @return a back-press decision that pops the top route with the result
     */
    public static NavBackPressResult pop(Serializable result) {
        return new NavBackPressResult(true, result);
    }

    /**
     * Decision to keep the current route: the back press was handled by the
     * route itself and the navigator must not pop anything.
     *
     * @return a back-press decision that keeps the route stack unchanged
     */
    public static NavBackPressResult noPop() {
        return new NavBackPressResult(false, null);
    }

    /**
     * @return true when the navigator should pop the top route, false when
     * the back press was fully consumed by the route
     */
    public boolean shouldPop() {
        return mShouldPop;
    }

    /**
     * @return the result to deliver to the pop callback, only meaningful
     * when {@link #shouldPop()} is true, may be null
     */
    public Serializable getResult() {
        return mResult;
    }
}
