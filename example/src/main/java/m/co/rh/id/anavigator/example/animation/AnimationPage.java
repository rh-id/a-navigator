package m.co.rh.id.anavigator.example.animation;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import m.co.rh.id.anavigator.RouteOptions;
import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.annotation.NavInject;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.example.R;

public class AnimationPage extends StatefulView<Activity> {

    @NavInject
    private transient INavigator mNavigator;

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        View rootLayout = activity.getLayoutInflater().inflate(R.layout.page_animation, null);
        Button animateImage = rootLayout.findViewById(R.id.button_animate_image);
        animateImage.setOnClickListener(view ->
                mNavigator.push((args, activity1) -> new AnimateImageDetailPage(),
                        null, null,
                        RouteOptions.withTransition(R.transition.page_animation_image_detail_enter, R.transition.page_animation_image_detail_exit))
        );
        return rootLayout;
    }
}
