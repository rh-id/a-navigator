package m.co.rh.id.anavigator.r8smoke;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Hosts the NavConfiguration (registered in SmokeApplication) and lets the
 * navigator handle the back button (mirrors the example module's
 * AppCompatExampleActivity).
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // this is required to let the navigator handle the back button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                SmokeApplication.of(MainActivity.this)
                        .getNavigator(MainActivity.this).onBackPressed();
            }
        });
    }
}
