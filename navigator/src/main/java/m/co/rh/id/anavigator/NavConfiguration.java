package m.co.rh.id.anavigator;


import android.app.Activity;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;

import java.util.Map;

import m.co.rh.id.anavigator.component.StatefulViewFactory;

public class NavConfiguration<ACT extends Activity, SV extends StatefulView> {
    private String initialRouteName;
    private Map<String, StatefulViewFactory<ACT, SV>> navMap;
    private Animation defaultInAnimation;
    private Animation defaultOutAnimation;

    /**
     * Same as {@link #NavConfiguration(String, Map, Animation, Animation)}
     * with inAnmation and outAnimation set to null
     */
    public NavConfiguration(String initialRouteName, Map<String, StatefulViewFactory<ACT, SV>> navMap) {
        this(initialRouteName, navMap, null, null);
    }

    /**
     * @param initialRouteName initial route to be pushed to navigator
     * @param navMap           mapping of the routes for this navigator
     * @param inAnimation      animation used when a view is shown/displayed
     * @param outAnimation     animation used when a view is hidden/removed from navigator
     */
    public NavConfiguration(String initialRouteName, Map<String, StatefulViewFactory<ACT, SV>> navMap, Animation inAnimation, Animation outAnimation) {
        if (initialRouteName == null) {
            throw new IllegalStateException("initial route name must not null!");
        }
        if (navMap == null || navMap.isEmpty()) {
            throw new IllegalStateException("navMap must not null or empty!");
        }
        this.initialRouteName = initialRouteName;
        this.navMap = navMap;

        defaultInAnimation = inAnimation;
        if (defaultInAnimation == null) {
            AnimationSet inAnimationSet = new AnimationSet(true);
            inAnimationSet.setInterpolator(new DecelerateInterpolator());
            inAnimationSet.setDuration(300);
            inAnimationSet.addAnimation(new AlphaAnimation(0, 1));
            inAnimationSet.addAnimation(new TranslateAnimation(0, 0, 100, 0));
            defaultInAnimation = inAnimationSet;
        }
        defaultOutAnimation = outAnimation;
        if (defaultOutAnimation == null) {
            AnimationSet outAnimationSet = new AnimationSet(true);
            outAnimationSet.setInterpolator(new AccelerateInterpolator());
            outAnimationSet.setDuration(50);
            outAnimationSet.addAnimation(new AlphaAnimation(1, 0));
            outAnimationSet.addAnimation(new TranslateAnimation(0, 0, 0, -100));
            defaultOutAnimation = outAnimationSet;
        }
    }

    public String getInitialRouteName() {
        return initialRouteName;
    }

    public Map<String, StatefulViewFactory<ACT, SV>> getNavMap() {
        return navMap;
    }

    Animation getDefaultInAnimation() {
        return defaultInAnimation;
    }

    Animation getDefaultOutAnimation() {
        return defaultOutAnimation;
    }
}
