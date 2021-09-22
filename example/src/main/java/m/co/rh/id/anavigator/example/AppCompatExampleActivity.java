package m.co.rh.id.anavigator.example;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Example to use the framework by extending AppCompatActivity
 */
public class AppCompatExampleActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();
        // this is required to let navigator handle the back button
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                MyApplication.of(AppCompatExampleActivity.this)
                        .getNavigator(AppCompatExampleActivity.this).onBackPressed();
            }
        });
    }
}
