package m.co.rh.id.anavigator.r8smoke;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Volatile holder for the injection outcomes of the minified-run harness.
 *
 * The instrumented test (in the separately minified test APK) reads ONLY this class.
 * It must never touch {@code SmokePage} / {@code SmokeChildPage} fields directly: a
 * direct reference from test code would keep those fields through the app APK's R8 run
 * and falsify the whole experiment.
 */
public final class SmokeResult {

    // Expected route index of SmokePage: it is pushed on top of LandingPage,
    // and the top of a 2-deep stack has route index 1 (index 0 is the default
    // field value, which would make the primitive check pass even with a
    // broken injection).
    public static final int EXPECTED_ROUTE_INDEX = 1;

    public static volatile boolean navigatorInjected = false;
    public static volatile boolean componentInjected = false;
    public static volatile boolean routeIndexWrapperOk = false;
    public static volatile boolean routeIndexPrimitiveOk = false;
    public static volatile boolean viewNavigatorInjected = false;
    public static volatile boolean childNavigatorInjected = false;

    private static final CountDownLatch LATCH = new CountDownLatch(1);

    private SmokeResult() {
    }

    /** Called by the harness pages once the outcomes are final. */
    public static void done() {
        LATCH.countDown();
    }

    public static void awaitDone(long timeout, TimeUnit unit) throws InterruptedException {
        LATCH.await(timeout, unit);
    }
}
