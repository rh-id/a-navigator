package m.co.rh.id.anavigator.component;

import android.app.Activity;
import android.view.View;

/**
 * implement this on StatefulView if you need to handle onBackPressed event from activity.
 * Navigator will forward onBackPressed event to ONLY current active StatefulView (currently displayed view)
 * <p>
 * The route declares its back outcome by returning a {@link NavBackPressResult}:
 * <ul>
 *     <li>{@link NavBackPressResult#pop()} / {@link NavBackPressResult#pop(Serializable)}
 *     - the navigator pops the route, honoring the carried result</li>
 *     <li>{@link NavBackPressResult#noPop()} - the route handled the back press
 *     itself (e.g. closed a drawer) and the route stack is kept</li>
 *     <li><b>null</b> - the route has no opinion, the navigator performs its
 *     default behavior (pop), identical to a route that does not implement
 *     this interface. The only way to swallow the back press is an explicit
 *     {@link NavBackPressResult#noPop()}</li>
 * </ul>
 * The framework owns the pop: for the synchronous case, a route that wants to
 * pop on back should return a pop decision instead of calling
 * {@code navigator.pop()} itself.
 */
public interface NavOnBackPressed<ACT extends Activity> {
    /**
     * when back button is pressed
     *
     * @param currentView current view of the StatefulView
     * @param activity    current activity
     * @param navigator   current instance of the navigator
     * @return the back-press decision of this route, or null to let the
     * navigator apply its default behavior (pop)
     */
    NavBackPressResult onBackPressed(View currentView, ACT activity, INavigator navigator);
}
