package m.co.rh.id.anavigator.example;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;

/**
 * Example to use the framework by extending Activity directly
 */
public class RawActivity extends Activity {

    @Override
    public void onBackPressed() {
        // this is required to let navigator handle the back button
        MyApplication.of(this).getNavigator(this).onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // this is required to let navigator handle onActivityResult
        MyApplication.of(this).getNavigator(this).onActivityResult(requestCode, resultCode, data);
    }
}