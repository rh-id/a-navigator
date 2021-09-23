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


@SuppressWarnings("rawtypes")
public class NavConfiguration<ACT extends Activity, SV extends StatefulView> {
    private String initialRouteName;
    private Map<String, StatefulViewFactory<ACT, SV>> navMap;
    private Animation defaultInAnimation;
    private Animation defaultOutAnimation;

    private NavConfiguration(String initialRouteName, Map<String, StatefulViewFactory<ACT, SV>> navMap) {
        if (initialRouteName == null) {
            throw new IllegalStateException("initial route name must not null!");
        }
        if (navMap == null || navMap.isEmpty()) {
            throw new IllegalStateException("navMap must not null or empty!");
        }
        this.initialRouteName = initialRouteName;
        this.navMap = navMap;
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

    public static class Builder<ACT extends Activity, SV extends StatefulView> {
        private String initialRouteName;
        private Map<String, StatefulViewFactory<ACT, SV>> navMap;
        private Animation inAnimation;
        private Animation outAnimation;

        /**
         * @param initialRouteName initial route to be pushed to navigator
         * @param navMap           mapping of the routes for this navigator
         */
        public Builder(String initialRouteName, Map<String, StatefulViewFactory<ACT, SV>> navMap) {
            this.initialRouteName = initialRouteName;
            this.navMap = navMap;
        }

        /**
         * animation used when a view is shown/displayed
         */
        public Builder setInAnimation(Animation inAnimation) {
            this.inAnimation = inAnimation;
            return this;
        }

        /**
         * animation used when a view is hidden/removed from navigator
         */
        public Builder setOutAnimation(Animation outAnimation) {
            this.outAnimation = outAnimation;
            return this;
        }

        public NavConfiguration<ACT, SV> build() {
            NavConfiguration<ACT, SV> navConfiguration = new NavConfiguration<>(initialRouteName, navMap);
            if (inAnimation == null) {
                AnimationSet inAnimationSet = new AnimationSet(true);
                inAnimationSet.setInterpolator(new DecelerateInterpolator());
                inAnimationSet.setDuration(300);
                inAnimationSet.addAnimation(new AlphaAnimation(0, 1));
                inAnimationSet.addAnimation(new TranslateAnimation(0, 0, 100, 0));
                inAnimation = inAnimationSet;
            }
            if (outAnimation == null) {
                AnimationSet outAnimationSet = new AnimationSet(true);
                outAnimationSet.setInterpolator(new AccelerateInterpolator());
                outAnimationSet.setDuration(50);
                outAnimationSet.addAnimation(new AlphaAnimation(1, 0));
                outAnimationSet.addAnimation(new TranslateAnimation(0, 0, 0, -100));
                outAnimation = outAnimationSet;
            }
            navConfiguration.defaultInAnimation = inAnimation;
            navConfiguration.defaultOutAnimation = outAnimation;

            return navConfiguration;
        }
    }
}
