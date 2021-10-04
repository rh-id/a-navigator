package m.co.rh.id.anavigator;


import android.app.Activity;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;

import java.io.File;
import java.util.Map;

import m.co.rh.id.anavigator.component.StatefulViewFactory;


@SuppressWarnings("rawtypes")
public class NavConfiguration<ACT extends Activity, SV extends StatefulView> {
    private String initialRouteName;
    private Map<String, StatefulViewFactory<ACT, SV>> navMap;
    private Animation defaultInAnimation;
    private Animation defaultOutAnimation;
    private File saveStateFile;

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

    public File getSaveStateFile() {
        return saveStateFile;
    }

    public static class Builder<ACT extends Activity, SV extends StatefulView> {
        private String initialRouteName;
        private Map<String, StatefulViewFactory<ACT, SV>> navMap;
        private Animation inAnimation;
        private Animation outAnimation;
        private File saveStateFile;

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

        /**
         * Provide file to save state, this file will be re-created and deleted as necessary.
         * <p>
         * the StatefulView states will be stored in this file by relying on java object serialization mechanism.
         * Use SealedObject class instead of default Serializable fields if you need to secure/encrypt them.
         * <p>
         * When app gets killed and re-opened, navigator will handle state restoration,
         * see https://developer.android.com/topic/libraries/architecture/saving-states
         * This saving states behave the same as Saved instance state option.
         * The states will be cleared only when activity is finishing properly.
         * <p>
         * NOTE: Make sure you have decent java serialization knowledge before using this.
         * Saving state can be quiet tricky to handle,
         * not to mention the states are not encrypted out of the box by this navigator
         */
        public Builder setSaveStateFile(File file) {
            this.saveStateFile = file;
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
            navConfiguration.saveStateFile = saveStateFile;
            return navConfiguration;
        }
    }
}
