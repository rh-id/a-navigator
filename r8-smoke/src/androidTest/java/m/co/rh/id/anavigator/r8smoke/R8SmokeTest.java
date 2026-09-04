package m.co.rh.id.anavigator.r8smoke;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Runs against the MINIFIED release APK (testBuildType 'release') to prove the
 * navigator AAR's consumer-rules.pro keep the reflection-based annotation
 * injection intact under R8.
 *
 * IMPORTANT: this test references ONLY SmokeResult. Referencing SmokePage (or
 * its fields) from the test APK would keep them through the app APK's R8 run
 * and falsify the experiment.
 */
@RunWith(AndroidJUnit4.class)
public class R8SmokeTest {

    private static final long AWAIT_TIMEOUT_SECONDS = 30;

    @Test
    public void annotatedFieldInjectionSurvivesR8Minification() throws Exception {
        ActivityScenario.launch(MainActivity.class);
        SmokeResult.awaitDone(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue("INavigator was not injected into @NavInject field "
                        + "(annotation stripped by R8?)",
                SmokeResult.navigatorInjected);
        assertTrue("ISmokeComponent was not injected into @NavInject field "
                        + "(annotation stripped by R8?)",
                SmokeResult.componentInjected);
        assertTrue("@NavRouteIndex Integer wrapper field was not injected with route index "
                        + SmokeResult.EXPECTED_ROUTE_INDEX,
                SmokeResult.routeIndexWrapperOk);
        assertTrue("@NavRouteIndex int primitive field was not injected with route index "
                        + SmokeResult.EXPECTED_ROUTE_INDEX,
                SmokeResult.routeIndexPrimitiveOk);
        assertTrue("@NavViewNavigator(\"smoke_container\") ViewNavigator was not injected "
                        + "(annotation stripped by R8?)",
                SmokeResult.viewNavigatorInjected);
        assertTrue("SmokeChildPage inside the smoke_container ViewNavigator was not injected",
                SmokeResult.childNavigatorInjected);
    }
}
