package m.co.rh.id.anavigator;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.example.MyApplication;
import m.co.rh.id.anavigator.example.RawActivity;
import m.co.rh.id.anavigator.example.Routes;
import m.co.rh.id.anavigator.example.R;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Instrumented tests for {@link Navigator#replace(String, Serializable, m.co.rh.id.anavigator.component.NavPopCallback, RouteOptions)}
 * and {@link Navigator#retry(Serializable)}.
 * <p>
 * They cover the reproduced bugs:
 * - replace() desyncing the view stack with transition-based RouteOptions
 * (stale displayed view, followed by IndexOutOfBoundsException on back-press),
 * - retry() displaying the bottom-of-stack view instead of the rebuilt route,
 * - replace() on the initial route leaving a dead view inside the view stack,
 * and the TransitionManager scene-map leak plus the saved-state consistency.
 */
@RunWith(AndroidJUnit4.class)
public class NavigatorTest {

    private static final long TIMEOUT_MS = 15000;
    private static final long QUIET_PERIOD_MS = 4500;

    /**
     * Navigator's NavViewLayout is set as the activity content view with this id.
     */
    private static final int NAV_ANIMATOR_ID = Integer.MAX_VALUE;

    private ActivityScenario<RawActivity> mScenario;
    private long mLaunchElapsed;

    @After
    public void tearDown() {
        if (mScenario != null) {
            mScenario.close();
            mScenario = null;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static INavigator navigator(RawActivity activity) {
        return MyApplication.of(activity).getNavigator(activity);
    }

    private static View displayedView(RawActivity activity) {
        ViewGroup animator = activity.findViewById(NAV_ANIMATOR_ID);
        if (animator == null || animator.getChildCount() == 0) {
            return null;
        }
        return animator.getChildAt(0);
    }

    private static boolean isHomeDisplayed(RawActivity activity) {
        View view = displayedView(activity);
        return view != null && view.findViewById(R.id.drawer) != null;
    }

    private static boolean isSecondDisplayed(RawActivity activity) {
        View view = displayedView(activity);
        return view != null && view.findViewById(R.id.button_plus) != null;
    }

    private void await(String message, Callable<Boolean> condition) throws Exception {
        long deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS;
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition.call()) {
                return;
            }
            Thread.sleep(100);
        }
        fail("Timed out waiting for: " + message);
    }

    /**
     * Launches RawActivity and resets the (process-wide) navigator to a single
     * HOME route, then waits out the SplashPage one-shot retry timer so test
     * bodies run on a quiet navigator.
     */
    private void launchAndResetToHome() throws Exception {
        mLaunchElapsed = SystemClock.elapsedRealtime();
        mScenario = ActivityScenario.launch(RawActivity.class);
        // wait until the navigator finished its (async) state restore/initial push:
        // the current route must exist and the displayed child must be stable
        // across two consecutive idle syncs (loading view replaced by content)
        await("navigator restored", () -> {
            AtomicBoolean ok = new AtomicBoolean(false);
            mScenario.onActivity(activity -> ok.set(navigator(activity).getCurrentRoute() != null
                    && displayedView(activity) != null));
            if (!ok.get()) {
                return false;
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Thread.sleep(300);
            AtomicReference<View> before = new AtomicReference<>();
            mScenario.onActivity(activity -> before.set(displayedView(activity)));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            AtomicBoolean stable = new AtomicBoolean(false);
            mScenario.onActivity(activity -> stable.set(before.get() == displayedView(activity)));
            return stable.get();
        });
        // pop leftovers from previous tests and end up with a single HOME route
        await("reset to single HOME route", () -> {
            AtomicBoolean ready = new AtomicBoolean(false);
            mScenario.onActivity(activity -> {
                INavigator nav = navigator(activity);
                while (!nav.isInitialRoute()) {
                    nav.pop();
                }
                if (!isHomeDisplayed(activity)) {
                    // fresh launches display the SplashPage initial route,
                    // replacing it with HOME is the same splash pattern used
                    // in production apps
                    nav.replace(Routes.HOME_PAGE, Boolean.TRUE, null, null);
                }
                ready.set(isHomeDisplayed(activity) && nav.isInitialRoute());
            });
            return ready.get();
        });
        // quiet period: let the one-shot splash retry timer (armed ~3s after
        // launch) fire before the actual test body starts
        while (SystemClock.elapsedRealtime() - mLaunchElapsed < QUIET_PERIOD_MS) {
            Thread.sleep(100);
        }
    }

    private static RouteOptions animationOptions() {
        return RouteOptions.withAnimation(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right);
    }

    private static RouteOptions transitionOptions() {
        return RouteOptions.withTransition(
                R.transition.page_animation_image_detail_enter,
                R.transition.page_animation_image_detail_exit);
    }

    // ---------------------------------------------------------------------------------------------
    // 1. Replace consistency for all three RouteOptions types
    // ---------------------------------------------------------------------------------------------

    @Test
    public void replace_nullOptions_displaysReplacedViewImmediately_andStackStaysConsistent()
            throws Exception {
        runReplaceConsistency(null);
    }

    @Test
    public void replace_animationOptions_displaysReplacedViewImmediately_andStackStaysConsistent()
            throws Exception {
        runReplaceConsistency(animationOptions());
    }

    @Test
    public void replace_transitionOptions_displaysReplacedViewImmediately_andStackStaysConsistent()
            throws Exception {
        runReplaceConsistency(transitionOptions());
    }

    private void runReplaceConsistency(RouteOptions options) throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push(Routes.SECOND_PAGE, null, null, options);
            assertTrue(isSecondDisplayed(activity));
            // the child swap must be synchronous: assert immediately
            nav.replace(Routes.HOME_PAGE, Boolean.TRUE, null, options);
            assertTrue("replaced route view must be displayed right after replace()",
                    isHomeDisplayed(activity));
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            // replace swaps the current route only, the route below stays
            assertFalse(nav.isInitialRoute());
            // the view stack must not be poisoned: a following push/pop round-trip
            // must not throw and must display the correct views
            nav.push(Routes.SECOND_PAGE, null, null, options);
            assertTrue(isSecondDisplayed(activity));
            assertTrue(nav.pop());
            assertTrue(isHomeDisplayed(activity));
            assertFalse(nav.isInitialRoute());
        });
    }

    // ---------------------------------------------------------------------------------------------
    // 2. Bug 1 reproduction: replace with transition options, then back-press
    // ---------------------------------------------------------------------------------------------

    @Test
    public void replace_withTransitionOptions_thenBackPress_noCrash_andHomeDisplayed()
            throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push(Routes.SECOND_PAGE, null, null, transitionOptions());
            assertTrue(isSecondDisplayed(activity));
            nav.replace(Routes.SECOND_PAGE, null, null, transitionOptions());
            assertTrue(isSecondDisplayed(activity));
            // this back-press/pop crashed with IndexOutOfBoundsException
            // (Index: -1) before the fix
            nav.onBackPressed();
            assertTrue(isHomeDisplayed(activity));
            assertTrue(nav.isInitialRoute());
        });
    }

    // ---------------------------------------------------------------------------------------------
    // 3. Bug 2 reproduction: retry() must rebuild the current route, not show
    // the bottom-of-stack view
    // ---------------------------------------------------------------------------------------------

    @Test
    public void retry_nullOptions_rebuildsCurrentRoute_notBottomOfStack() throws Exception {
        runRetryRebuildsCurrentView(null);
    }

    @Test
    public void retry_animationOptions_rebuildsCurrentRoute_notBottomOfStack() throws Exception {
        runRetryRebuildsCurrentView(animationOptions());
    }

    @Test
    public void retry_transitionOptions_rebuildsCurrentRoute_notBottomOfStack() throws Exception {
        runRetryRebuildsCurrentView(transitionOptions());
    }

    private void runRetryRebuildsCurrentView(RouteOptions options) throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push(Routes.SECOND_PAGE, null, null, options);
            assertTrue(isSecondDisplayed(activity));
            // increment the counter so a rebuilt page (counter = 0) is
            // distinguishable from the stale one
            View displayed = displayedView(activity);
            displayed.findViewById(R.id.button_plus).performClick();
            TextView textView = displayed.findViewById(R.id.textview_second);
            assertEquals(activity.getString(R.string.second_page, 1),
                    textView.getText().toString());
            nav.retry(null);
            assertTrue("retry must display the rebuilt route view, not the bottom view",
                    isSecondDisplayed(activity));
            TextView rebuiltTextView = displayedView(activity).findViewById(R.id.textview_second);
            assertEquals("retry must rebuild the StatefulView (counter reset to 0)",
                    activity.getString(R.string.second_page, 0),
                    rebuiltTextView.getText().toString());
            assertEquals(Routes.SECOND_PAGE, nav.getCurrentRoute().getRouteName());
            assertFalse(nav.isInitialRoute());
        });
    }

    // ---------------------------------------------------------------------------------------------
    // 4. Latent bug 3: replace on the initial route (splash-style)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void replaceOnInitialRoute_yieldsSingleRoute_andNavigationKeepsWorking()
            throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            // make sure the current single route is not HOME so the next
            // replace is guaranteed to be a replace-on-initial
            nav.replace(Routes.SECOND_PAGE, null, null, null);
            assertTrue(isSecondDisplayed(activity));
            assertTrue(nav.isInitialRoute());
            // splash-style: replace the initial route
            nav.replace(Routes.HOME_PAGE, Boolean.TRUE, null, null);
            assertTrue("replaced initial route must be displayed", isHomeDisplayed(activity));
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            assertTrue("replacing the initial route must keep it as the initial route",
                    nav.isInitialRoute());
            // latent bug 3: a following push/pop crashed with
            // IndexOutOfBoundsException (Index: 2, Size: 2) before the fix
            nav.push(Routes.SECOND_PAGE, null, null, null);
            assertTrue(isSecondDisplayed(activity));
            assertTrue(nav.pop());
            assertTrue(isHomeDisplayed(activity));
            assertTrue(nav.isInitialRoute());
        });
        // back-press on the replaced initial route must finish the activity
        mScenario.onActivity(activity -> navigator(activity).onBackPressed());
        await("activity finished after back-press on replaced initial route",
                () -> mScenario.getState() == androidx.lifecycle.Lifecycle.State.DESTROYED);
    }

    // ---------------------------------------------------------------------------------------------
    // 5. Dialog flows
    // ---------------------------------------------------------------------------------------------

    @Test
    public void replace_pageToDialog_thenDialogToPage() throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            MyApplication app = MyApplication.of(activity);
            String confirmRoute = app.getNavExtDialogConfig().route_confirmDialog();
            Serializable confirmArgs = app.getNavExtDialogConfig()
                    .args_confirmDialog("title", "message");
            // page -> dialog (also replaces the initial route)
            nav.replace(confirmRoute, confirmArgs, null, null);
            assertTrue(nav.getCurrentRoute().getStatefulView() instanceof StatefulViewDialog);
            assertTrue("dialog replace must not touch the displayed page child",
                    isHomeDisplayed(activity));
            assertTrue(nav.isInitialRoute());
            // dialog -> page
            nav.replace(Routes.HOME_PAGE, Boolean.TRUE, null, null);
            assertTrue(isHomeDisplayed(activity));
            assertTrue(nav.isInitialRoute());
            // view stack integrity after dialog replaces
            nav.push(Routes.SECOND_PAGE, null, null, null);
            assertTrue(isSecondDisplayed(activity));
            assertTrue(nav.pop());
            assertTrue(isHomeDisplayed(activity));
            assertTrue(nav.isInitialRoute());
        });
    }

    @Test
    public void retry_onDialogRoute_rebuildsTheDialog() throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            MyApplication app = MyApplication.of(activity);
            Serializable confirmArgs = app.getNavExtDialogConfig()
                    .args_confirmDialog("title", "message");
            nav.push(app.getNavExtDialogConfig().route_confirmDialog(), confirmArgs, null, null);
            StatefulView before = nav.getCurrentRoute().getStatefulView();
            assertTrue(before instanceof StatefulViewDialog);
            assertTrue("dialog route must not change the displayed page child",
                    isHomeDisplayed(activity));
            nav.retry();
            StatefulView after = nav.getCurrentRoute().getStatefulView();
            assertTrue(after instanceof StatefulViewDialog);
            assertNotSame("retry must rebuild the dialog StatefulView", before, after);
            assertTrue(isHomeDisplayed(activity));
            nav.pop();
            assertTrue(isHomeDisplayed(activity));
            assertTrue(nav.isInitialRoute());
        });
    }

    // ---------------------------------------------------------------------------------------------
    // 6. Saved-state consistency: after replace, the old route must be gone
    // ---------------------------------------------------------------------------------------------

    @Test
    public void replace_oldRoutePurgedFromSavedState_afterRecreate() throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push(Routes.SECOND_PAGE, null, null, null);
            assertTrue(isSecondDisplayed(activity));
            // replaces SECOND with HOME: the saved state must contain [HOME] only
            nav.replace(Routes.HOME_PAGE, Boolean.TRUE, null, null);
            assertTrue(isHomeDisplayed(activity));
        });
        // recreate (not finishing): the navigator must restore the last saved
        // state, which must contain [HOME(below), HOME(replaced)] and no
        // longer the replaced SECOND route
        mScenario.recreate();
        await("restored state shows the replaced stack", () -> {
            AtomicBoolean ok = new AtomicBoolean(false);
            mScenario.onActivity(activity -> {
                INavigator nav = navigator(activity);
                ok.set(nav.getCurrentRoute() != null
                        && isHomeDisplayed(activity)
                        && Routes.HOME_PAGE.equals(nav.getCurrentRoute().getRouteName())
                        && !nav.isInitialRoute());
            });
            return ok.get();
        });
    }

    // ---------------------------------------------------------------------------------------------
    // 7. Same-frame rapid operations with transition options
    // ---------------------------------------------------------------------------------------------

    @Test
    public void sameFrame_pushThenReplace_withTransitions_noCrash_andReplacedViewDisplayed()
            throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            // issued back-to-back within the same main-loop iteration
            nav.push(Routes.SECOND_PAGE, null, null, transitionOptions());
            nav.replace(Routes.HOME_PAGE, Boolean.TRUE, null, transitionOptions());
            assertTrue(isHomeDisplayed(activity));
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            // replace swaps the current route only, the route below stays
            assertFalse(nav.isInitialRoute());
            // and the navigator stays usable
            nav.push(Routes.SECOND_PAGE, null, null, transitionOptions());
            assertTrue(isSecondDisplayed(activity));
            assertTrue(nav.pop());
            assertTrue(isHomeDisplayed(activity));
        });
    }
}

