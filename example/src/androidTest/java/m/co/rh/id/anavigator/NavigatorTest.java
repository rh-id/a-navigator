package m.co.rh.id.anavigator;

import android.app.Activity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.NavBackEvent;
import m.co.rh.id.anavigator.component.NavBackPressResult;
import m.co.rh.id.anavigator.component.NavOnBackPressed;
import m.co.rh.id.anavigator.component.NavOnRouteChangedListener;
import m.co.rh.id.anavigator.example.MyApplication;
import m.co.rh.id.anavigator.example.RawActivity;
import m.co.rh.id.anavigator.example.Routes;
import m.co.rh.id.anavigator.example.R;
import m.co.rh.id.anavigator.example.SecondPage;
import m.co.rh.id.anavigator.example.ThirdPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
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
 * and the framework TransitionManager scene-to-transition map leak plus the
 * saved-state consistency.
 */
@RunWith(AndroidJUnit4.class)
public class NavigatorTest {

    private static final long TIMEOUT_MS = 15000;
    private static final long QUIET_PERIOD_MS = 4500;

    /**
     * View tag marking the root view of the test-only scripted back page.
     */
    private static final String TAG_SCRIPTED_BACK_PAGE = "scripted_back_page";
    /**
     * Result scripted into the {@link NavBackPressResult#pop(Serializable)}
     * decision of the test-only scripted back page.
     */
    private static final String TAG_SCRIPTED_BACK_RESULT = "scripted_back_result";

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

    // ---------------------------------------------------------------------------------------------
    // 8. Predictive back gesture state machine
    // ---------------------------------------------------------------------------------------------

    /**
     * Gesture event factory: touch point on the LEFT edge of the screen
     * (progress 0 start), matching the physical back gesture.
     */
    private static NavBackEvent gestureStartEvent() {
        return new NavBackEvent(10f, 500f, 0f);
    }

    private static NavBackEvent gestureProgressEvent(float progress) {
        return new NavBackEvent(10f, 500f, progress);
    }

    private static NavViewLayout navLayout(RawActivity activity) {
        return (NavViewLayout) activity.findViewById(NAV_ANIMATOR_ID);
    }

    private static int attachedChildCount(RawActivity activity) {
        ViewGroup animator = activity.findViewById(NAV_ANIMATOR_ID);
        return animator == null ? 0 : animator.getChildCount();
    }

    @Test
    public void predictiveBack_shouldInterceptBack_truthTable() throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            // initial route is HOME_PAGE which implements NavOnBackPressed
            assertTrue(nav.isInitialRoute());
            assertTrue("stack==1 with NavOnBackPressed page must intercept back",
                    nav.shouldInterceptBack());
            // gesture at stack==1 must NOT attach anything (no animation, no flag)
            nav.onBackStarted(gestureStartEvent());
            assertEquals(1, attachedChildCount(activity));
            nav.onBackCancelled();
            // initial route WITHOUT NavOnBackPressed
            nav.replace(Routes.SECOND_PAGE, null, null, null);
            assertTrue(nav.isInitialRoute());
            assertFalse("stack==1 without NavOnBackPressed page must NOT intercept back",
                    nav.shouldInterceptBack());
            // stack size > 1
            nav.push(Routes.SECOND_PAGE, null, null, null);
            assertTrue("stack>1 must intercept back", nav.shouldInterceptBack());
            assertTrue(nav.pop());
            // dialog route on top
            MyApplication app = MyApplication.of(activity);
            Serializable confirmArgs = app.getNavExtDialogConfig()
                    .args_confirmDialog("title", "message");
            nav.push(app.getNavExtDialogConfig().route_confirmDialog(), confirmArgs, null, null);
            assertTrue(nav.getCurrentRoute().getStatefulView() instanceof StatefulViewDialog);
            assertFalse("dialog route on top must NOT intercept back (dialog window"
                    + " consumes back itself)", nav.shouldInterceptBack());
            nav.pop();
            assertTrue(isSecondDisplayed(activity));
        });
    }

    @Test
    public void predictiveBack_startedProgressedCommit_popsRouteOnce_andDisplaysPreviousPage()
            throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push(Routes.SECOND_PAGE, null, null, null);
            assertTrue(isSecondDisplayed(activity));
            // gesture start: with lazy arming it must NOT attach anything
            // yet (Android 16 synthetic sessions can dangle after started)
            nav.onBackStarted(gestureStartEvent());
            assertEquals("gesture start must NOT arm the speculative pop",
                    1, attachedChildCount(activity));
            // the first progress > 0 arms the gesture lazily: the previous
            // page view is attached underneath
            nav.onBackProgressed(gestureProgressEvent(0.4f));
            assertEquals("first progress > 0 must attach the previous page"
                    + " view beneath", 2, attachedChildCount(activity));
            // gesture progress drives the animation transforms
            nav.onBackProgressed(gestureProgressEvent(1f));
            assertEquals(2, attachedChildCount(activity));
            // commit: exactly one pop, the previous page displayed, no standard
            // pop animation (suppressed) and no crash
            nav.onBackPressed();
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            assertTrue(nav.isInitialRoute());
            assertTrue(isHomeDisplayed(activity));
        });
        // the commit animation detaches the popped view asynchronously
        await("commit animation removed the popped view", () -> {
            AtomicBoolean ok = new AtomicBoolean(false);
            mScenario.onActivity(activity ->
                    ok.set(attachedChildCountSync(activity) == 1));
            return ok.get();
        });
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            // navigator must stay fully usable after the gesture commit
            nav.push(Routes.SECOND_PAGE, null, null, null);
            assertTrue(isSecondDisplayed(activity));
            assertTrue(nav.pop());
            assertTrue(isHomeDisplayed(activity));
            assertTrue(nav.isInitialRoute());
        });
    }

    /**
     * Synchronous child-count read usable inside await().
     */
    private int attachedChildCountSync(RawActivity activity) {
        ViewGroup animator = activity.findViewById(NAV_ANIMATOR_ID);
        return animator == null ? 0 : animator.getChildCount();
    }

    @Test
    public void predictiveBack_startedThenCancelled_routeIntact_singleChildRestored()
            throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push(Routes.SECOND_PAGE, null, null, null);
            assertTrue(isSecondDisplayed(activity));
            nav.onBackStarted(gestureStartEvent());
            nav.onBackProgressed(gestureProgressEvent(0.7f));
            assertEquals(2, attachedChildCount(activity));
            // gesture released below the commit threshold
            nav.onBackCancelled();
            // route stack untouched
            assertEquals(Routes.SECOND_PAGE, nav.getCurrentRoute().getRouteName());
            assertFalse(nav.isInitialRoute());
            // NOTE: the display check must NOT run here - the revealed home
            // view is detached only by the ~180ms restore-animation end action
        });
        // the cancel animation detaches the previous view asynchronously
        await("cancel animation restored the single child", () -> {
            AtomicBoolean ok = new AtomicBoolean(false);
            mScenario.onActivity(activity ->
                    ok.set(attachedChildCountSync(activity) == 1));
            return ok.get();
        });
        mScenario.onActivity(activity -> {
            // once the restore finished, the interrupted page is displayed again
            assertTrue(isSecondDisplayed(activity));
            // a regular back press after the cancelled gesture still pops
            navigator(activity).onBackPressed();
            assertEquals(Routes.HOME_PAGE, navigator(activity).getCurrentRoute().getRouteName());
            assertTrue(navigator(activity).isInitialRoute());
        });
    }

    @Test
    public void predictiveBack_commitWithoutStarted_legacyPop() throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push(Routes.SECOND_PAGE, null, null, null);
            assertTrue(isSecondDisplayed(activity));
            // 3-button-navigation path: back press without any gesture event,
            // must behave exactly like the legacy pop
            nav.onBackPressed();
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            assertTrue(nav.isInitialRoute());
            assertTrue(isHomeDisplayed(activity));
            assertEquals(1, attachedChildCount(activity));
        });
    }

    @Test
    public void predictiveBack_danglingStartedSession_neverArms_andNextCommitPopsCleanly()
            throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push(Routes.SECOND_PAGE, null, null, null);
            assertTrue(isSecondDisplayed(activity));
            // Android 16 synthetic dangling session: onBackStarted arrives
            // (progress 0.0) but neither a commit nor a cancel EVER follows.
            // With lazy arming the session must not touch the view hierarchy.
            nav.onBackStarted(gestureStartEvent());
            assertEquals("a dangling started session must NOT arm the"
                    + " speculative pop (no previous view attached beneath)",
                    1, attachedChildCount(activity));
            assertEquals(Routes.SECOND_PAGE, nav.getCurrentRoute().getRouteName());
            assertFalse(nav.isInitialRoute());
            // (no progress, no cancel - the session dangles)
            // the plain back press that follows must pop cleanly through the
            // legacy path because mPredictiveBackActive stayed false
            nav.onBackPressed();
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            assertTrue(nav.isInitialRoute());
            assertEquals(1, attachedChildCount(activity));
            assertTrue(isHomeDisplayed(activity));
        });
    }

    @Test
    public void predictiveBack_interceptorPage_noAnimation_commitDeliversPageBackHandling()
            throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            // push a HOME_PAGE instance ON TOP of home: it implements
            // NavOnBackPressed, so the gesture must NOT animate a speculative
            // pop (the page's back behavior is unknown)
            nav.push(Routes.HOME_PAGE, Boolean.TRUE, null, null);
            assertTrue(nav.getCurrentRoute().getStatefulView() instanceof NavOnBackPressed);
            assertFalse(nav.isInitialRoute());
            nav.onBackStarted(gestureStartEvent());
            nav.onBackProgressed(gestureProgressEvent(0.5f));
            assertEquals("no speculative pop animation may run for an interceptor page",
                    1, attachedChildCount(activity));
            // commit still delivers the legacy NavOnBackPressed semantics:
            // HOME closes the drawer or pops
            nav.onBackPressed();
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            assertTrue(nav.isInitialRoute());
            assertTrue(isHomeDisplayed(activity));
            assertEquals(1, attachedChildCount(activity));
        });
    }

    @Test
    public void predictiveBack_popIssuedDuringGesture_autoCancels_noLeakedChild()
            throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push(Routes.SECOND_PAGE, null, null, null);
            nav.onBackStarted(gestureStartEvent());
            // first progress > 0 arms the gesture (lazy arming: started no
            // longer attaches anything)
            nav.onBackProgressed(gestureProgressEvent(0.4f));
            assertEquals(2, attachedChildCount(activity));
            // a programmatic pop lands while the gesture is in flight: the
            // navigator must auto-cancel the gesture and complete the pop
            assertTrue(nav.pop());
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            assertTrue(nav.isInitialRoute());
            assertTrue(isHomeDisplayed(activity));
        });
        await("auto-cancelled gesture left a single attached child", () -> {
            AtomicBoolean ok = new AtomicBoolean(false);
            mScenario.onActivity(activity ->
                    ok.set(attachedChildCountSync(activity) == 1));
            return ok.get();
        });
        mScenario.onActivity(activity -> {
            // the navigator must stay fully usable: a following push/pop
            // round-trip proves no leaked child poisons the view stack
            INavigator nav = navigator(activity);
            nav.push(Routes.SECOND_PAGE, null, null, null);
            assertTrue(isSecondDisplayed(activity));
            assertTrue(nav.pop());
            assertTrue(isHomeDisplayed(activity));
        });
    }

    @Test
    public void predictiveBack_pushIssuedDuringGesture_autoCancels_noLeakedChild()
            throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push(Routes.SECOND_PAGE, null, null, null);
            nav.onBackStarted(gestureStartEvent());
            // first progress > 0 arms the gesture (lazy arming: started no
            // longer attaches anything)
            nav.onBackProgressed(gestureProgressEvent(0.4f));
            assertEquals(2, attachedChildCount(activity));
            // a push lands while the gesture is in flight: auto-cancel then
            // complete the push (ThirdPage is an anonymous route)
            nav.push((args, activity1) -> new ThirdPage(), null, null, null);
            // NOTE: the display check must NOT run here - children are
            // [home, third] until the deferred restore end action removes home
        });
        await("auto-cancelled gesture left a single attached child after push", () -> {
            AtomicBoolean ok = new AtomicBoolean(false);
            mScenario.onActivity(activity ->
                    ok.set(attachedChildCountSync(activity) == 1));
            return ok.get();
        });
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            assertTrue("ThirdPage must be displayed after the push landed and"
                    + " the restore animation completed", isThirdDisplayed(activity));
            // pop twice back to home proves the stacks stayed consistent
            assertTrue(nav.pop());
            assertTrue(nav.pop());
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            assertTrue(nav.isInitialRoute());
            assertTrue(isHomeDisplayed(activity));
        });
    }

    @Test
    public void predictiveBack_progressedWithoutStarted_normalizes_andCommitPopsOnce()
            throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push(Routes.SECOND_PAGE, null, null, null);
            nav.push((args, activity1) -> new SecondPage(), null, null, null);
            assertTrue(isSecondDisplayed(activity));
            // progressed delivered WITHOUT a preceding started: this is now
            // the PRIMARY arming path (lazy arming at the first progress > 0,
            // onBackStarted is a deliberate no-op against dangling synthetic
            // sessions), no crash
            nav.onBackProgressed(gestureProgressEvent(0.5f));
            assertEquals("first progress > 0 must attach the previous page view"
                            + " beneath the current one",
                    2, attachedChildCount(activity));
            // route intact: [HOME, SECOND#1, SECOND#2(anonymous top)] -
            // the top route is the anonymous one, assert by stack position
            assertEquals(2, nav.findRouteIndex(nav.getCurrentRoute()));
            assertFalse(nav.isInitialRoute());
            // commit pops exactly once: top page gone, the NAMED second page
            // below it is the new current route
            nav.onBackPressed();
            assertEquals(Routes.SECOND_PAGE, nav.getCurrentRoute().getRouteName());
            assertFalse(nav.isInitialRoute());
            View currentView = navLayout(activity).getCurrentView();
            assertTrue("the page below the popped one must be the current view",
                    currentView != null
                            && currentView.findViewById(R.id.button_plus) != null);
        });
        // the commit animation detaches the popped view asynchronously, the
        // single-child invariant leaves only the new current page attached
        await("commit animation settled to the single remaining child", () -> {
            AtomicBoolean ok = new AtomicBoolean(false);
            mScenario.onActivity(activity ->
                    ok.set(attachedChildCountSync(activity) == 1));
            return ok.get();
        });
        mScenario.onActivity(activity -> {
            // navigator must stay fully usable
            INavigator nav = navigator(activity);
            assertTrue(nav.pop());
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            assertTrue(nav.isInitialRoute());
            assertTrue(isHomeDisplayed(activity));
        });
    }

    @Test
    public void predictiveBack_replaceIssuedDuringGesture_autoCancels_noLeakedChild()
            throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push(Routes.SECOND_PAGE, null, null, null);
            nav.onBackStarted(gestureStartEvent());
            // first progress > 0 arms the gesture (lazy arming: started no
            // longer attaches anything)
            nav.onBackProgressed(gestureProgressEvent(0.4f));
            assertEquals(2, attachedChildCount(activity));
            // a replace lands while the gesture is in flight: the navigator
            // must auto-cancel the gesture and complete the replace
            nav.replace(Routes.SECOND_PAGE, null, null, null);
            assertEquals(Routes.SECOND_PAGE, nav.getCurrentRoute().getRouteName());
            assertFalse(nav.isInitialRoute());
        });
        await("replace during gesture left a single attached child", () -> {
            AtomicBoolean ok = new AtomicBoolean(false);
            mScenario.onActivity(activity ->
                    ok.set(attachedChildCountSync(activity) == 1));
            return ok.get();
        });
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            // the replaced route must be displayed once the restore settled
            assertTrue("replaced route must be displayed after the gesture"
                    + " auto-cancel settled", isSecondDisplayed(activity));
            // navigator must stay fully usable
            assertTrue(nav.pop());
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            assertTrue(nav.isInitialRoute());
            assertTrue(isHomeDisplayed(activity));
        });
    }

    @Test
    public void predictiveBack_secondPopInsideCommitWindow_popCallbackReceivesCorrectView()
            throws Exception {
        launchAndResetToHome();
        final AtomicReference<View> firstPopViewRef = new AtomicReference<>();
        final AtomicReference<View> secondPopViewRef = new AtomicReference<>();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            // stack: [HOME, SECOND#1, SECOND#2]; SECOND#2 on top carries the
            // first callback (popped by the commit), SECOND#1 the second one
            // (popped inside the commit window)
            nav.push(Routes.SECOND_PAGE, null, (navigator, navRoute, activity1, currentView) ->
                    firstPopViewRef.set(currentView));
            nav.push((args, activity1) -> new SecondPage(), null,
                    (navigator, navRoute, activity1, currentView) ->
                            secondPopViewRef.set(currentView));
            assertTrue(isSecondDisplayed(activity));
            // gesture on the top page, then commit
            nav.onBackStarted(gestureStartEvent());
            // first progress > 0 arms the gesture (lazy arming: started no
            // longer attaches anything)
            nav.onBackProgressed(gestureProgressEvent(0.4f));
            assertEquals(2, attachedChildCount(activity));
            nav.onBackPressed();
            assertEquals(Routes.SECOND_PAGE, nav.getCurrentRoute().getRouteName());
            assertFalse(nav.isInitialRoute());
            // second pop issued synchronously inside the ~200ms commit window:
            // the popped page's NavPopCallback must receive the TRUE current
            // view (the page being popped), NOT the animating-out view of the
            // commit - this is the regression the stack-filtered
            // NavViewLayout.getCurrentView() protects
            assertTrue(nav.pop());
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            assertTrue(nav.isInitialRoute());
        });
        await("double pop settled to a single attached child", () -> {
            AtomicBoolean ok = new AtomicBoolean(false);
            mScenario.onActivity(activity ->
                    ok.set(attachedChildCountSync(activity) == 1));
            return ok.get();
        });
        mScenario.onActivity(activity -> {
            assertTrue(isHomeDisplayed(activity));
            assertNotNull("commit pop callback must receive a view",
                    firstPopViewRef.get());
            assertNotNull("second pop callback must receive a view",
                    secondPopViewRef.get());
            assertNotSame("second pop inside the commit window must receive the"
                            + " true current view, not the animating-out view of the commit",
                    firstPopViewRef.get(), secondPopViewRef.get());
        });
    }

    private static boolean isThirdDisplayed(RawActivity activity) {
        View view = displayedView(activity);
        return view != null && view.findViewById(R.id.textview_third) != null;
    }

    private static boolean isScriptedBackDisplayed(RawActivity activity) {
        View view = displayedView(activity);
        return view != null && TAG_SCRIPTED_BACK_PAGE.equals(view.getTag());
    }

    /**
     * Minimal test-only route implementing {@link NavOnBackPressed} that
     * returns a scripted {@link NavBackPressResult} decision, so each
     * contract case (pop with result, noPop, null = default) can be driven
     * against a real navigator without depending on the example pages.
     */
    private static class ScriptedBackPage extends StatefulView<Activity> implements NavOnBackPressed {
        static final int MODE_POP_WITH_RESULT = 1;
        static final int MODE_NO_POP = 2;
        static final int MODE_NULL = 3;
        private int mMode;

        void setMode(int mode) {
            mMode = mode;
        }

        @Override
        protected View createView(Activity activity, ViewGroup container) {
            TextView textView = new TextView(activity);
            textView.setText("ScriptedBackPage");
            textView.setTag(TAG_SCRIPTED_BACK_PAGE);
            return textView;
        }

        @Override
        public NavBackPressResult onBackPressed(View currentView, Activity activity, INavigator navigator) {
            switch (mMode) {
                case MODE_POP_WITH_RESULT:
                    return NavBackPressResult.pop(TAG_SCRIPTED_BACK_RESULT);
                case MODE_NO_POP:
                    return NavBackPressResult.noPop();
                case MODE_NULL:
                default:
                    // no opinion: the navigator must apply its default (pop),
                    // identical to a route that does not implement the interface
                    return null;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 9. NavOnBackPressed decision contract (back-press dispatch)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void backPress_popResult_backPopsRoute_popCallbackReceivesResult() throws Exception {
        launchAndResetToHome();
        final AtomicReference<NavRoute> poppedRouteRef = new AtomicReference<>();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push((args, activity1) -> new ScriptedBackPage(), null,
                    (navigator, navRoute, activity1, currentView) ->
                            poppedRouteRef.set(navRoute));
            assertTrue(isScriptedBackDisplayed(activity));
            ScriptedBackPage page = (ScriptedBackPage) nav.getCurrentRoute().getStatefulView();
            page.setMode(ScriptedBackPage.MODE_POP_WITH_RESULT);
            // back press: the route's pop decision must pop it and the
            // scripted result must be delivered to the popped NavRoute
            nav.onBackPressed();
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            assertTrue(nav.isInitialRoute());
            assertTrue(isHomeDisplayed(activity));
            assertNotNull("pop callback must have run", poppedRouteRef.get());
            assertEquals("pop callback must deliver the scripted result",
                    TAG_SCRIPTED_BACK_RESULT, poppedRouteRef.get().getRouteResult());
        });
    }

    @Test
    public void backPress_noPop_stackUnchanged() throws Exception {
        launchAndResetToHome();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push((args, activity1) -> new ScriptedBackPage(), null, null);
            assertTrue(isScriptedBackDisplayed(activity));
            ScriptedBackPage page = (ScriptedBackPage) nav.getCurrentRoute().getStatefulView();
            page.setMode(ScriptedBackPage.MODE_NO_POP);
            // back press: an explicit noPop must swallow the back press and
            // keep the route stack (and the displayed view) unchanged
            nav.onBackPressed();
            assertFalse(nav.isInitialRoute());
            assertTrue("noPop must keep the scripted page on the stack",
                    isScriptedBackDisplayed(activity));
            assertEquals("noPop must not attach/detach anything",
                    1, attachedChildCount(activity));
            // the navigator stays fully usable: a following pop still works
            assertTrue(nav.pop());
            assertTrue(nav.isInitialRoute());
            assertTrue(isHomeDisplayed(activity));
        });
    }

    @Test
    public void backPress_nullResult_defaultPopWithNullResult() throws Exception {
        launchAndResetToHome();
        final AtomicReference<NavRoute> poppedRouteRef = new AtomicReference<>();
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.push((args, activity1) -> new ScriptedBackPage(), null,
                    (navigator, navRoute, activity1, currentView) ->
                            poppedRouteRef.set(navRoute));
            assertTrue(isScriptedBackDisplayed(activity));
            ScriptedBackPage page = (ScriptedBackPage) nav.getCurrentRoute().getStatefulView();
            page.setMode(ScriptedBackPage.MODE_NULL);
            // back press: a null decision means "no opinion", the navigator
            // must apply its default behavior (pop) exactly like for a route
            // that does not implement NavOnBackPressed
            nav.onBackPressed();
            assertEquals(Routes.HOME_PAGE, nav.getCurrentRoute().getRouteName());
            assertTrue(nav.isInitialRoute());
            assertTrue(isHomeDisplayed(activity));
            assertNotNull("pop callback must have run", poppedRouteRef.get());
            assertNull("default pop must carry no result",
                    poppedRouteRef.get().getRouteResult());
        });
    }

    // ---------------------------------------------------------------------------------------------
    // 10. Saved-state restore announces the restored stack to route listeners
    // ---------------------------------------------------------------------------------------------

    @Test
    public void restore_announcesRouteChangedWithNullPrevious_andBackStateSyncs()
            throws Exception {
        launchAndResetToHome();
        final List<NavRoute[]> events = new ArrayList<>();
        // listener and its registration survive recreate() through the
        // application-scoped navigator, exactly like the example activity's
        // dispatcher-callback sync listener
        final NavOnRouteChangedListener listener = (previous, current) ->
                events.add(new NavRoute[]{previous, current});
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            nav.addOnRouteChangedListener(listener);
            // multi-page stack so the restored state has something to announce
            nav.push(Routes.SECOND_PAGE, null, null, null);
            assertTrue(isSecondDisplayed(activity));
        });
        // only the RESTORE announcement matters, drop the push event
        mScenario.onActivity(activity -> events.clear());
        // same restore mechanics as replace_oldRoutePurgedFromSavedState_..
        // : recreate() re-runs onActivityCreated -> initNavigatorState, which
        // replaces the route stack wholesale from the saved snapshot
        mScenario.recreate();
        await("restored [HOME, SECOND] stack after recreate", () -> {
            AtomicBoolean ok = new AtomicBoolean(false);
            mScenario.onActivity(activity -> {
                INavigator nav = navigator(activity);
                ok.set(nav.getCurrentRoute() != null
                        && Routes.SECOND_PAGE.equals(nav.getCurrentRoute().getRouteName())
                        && !nav.isInitialRoute()
                        && isSecondDisplayed(activity));
            });
            return ok.get();
        });
        mScenario.onActivity(activity -> {
            INavigator nav = navigator(activity);
            // (a) exactly ONE announcement, previous == null, current = the
            // restored top route (second element of the recorded pair)
            assertEquals("restore must fire exactly one route-changed event",
                    1, events.size());
            assertNull("restore event previous must be null", events.get(0)[0]);
            assertNotNull("restore event current must be the restored top route",
                    events.get(0)[1]);
            assertEquals(Routes.SECOND_PAGE, events.get(0)[1].getRouteName());
            // (b) the dispatcher-callback sync predicate reflects the
            // restored stack: [HOME, SECOND], SECOND has no custom back
            // handling, stack size > 1 -> must intercept
            assertTrue("restored [HOME, SECOND] stack must intercept back",
                    nav.shouldInterceptBack());
        });
        // listener MUST be removed: the app-level navigator outlives the test
        mScenario.onActivity(activity ->
                navigator(activity).removeOnRouteChangedListener(listener));
    }
}

