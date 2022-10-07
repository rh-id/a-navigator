package m.co.rh.id.anavigator.example.animation;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.example.R;

public class AnimateImageDetailPage extends StatefulView<Activity> {

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        return activity.getLayoutInflater().inflate(R.layout.page_animate_image_detail, null);
    }
}
