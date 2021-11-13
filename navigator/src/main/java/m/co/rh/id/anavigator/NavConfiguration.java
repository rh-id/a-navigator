package m.co.rh.id.anavigator;


import android.app.Activity;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;

import java.io.File;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.NullCipher;

import m.co.rh.id.anavigator.component.StatefulViewFactory;


@SuppressWarnings("rawtypes")
public class NavConfiguration<ACT extends Activity, SV extends StatefulView> {
    private String initialRouteName;
    private Map<String, StatefulViewFactory<ACT, SV>> navMap;
    private Animation defaultEnterAnimation;
    private Animation defaultExitAnimation;
    private Animation defaultPopEnterAnimation;
    private Animation defaultPopExitAnimation;
    private Animation defaultReBuildEnterAnimation;
    private Animation defaultReBuildExitAnimation;
    private File saveStateFile;
    private Cipher saveStateEncryptCipher;
    private Cipher saveStateDecryptCipher;

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

    Animation getDefaultEnterAnimation() {
        return defaultEnterAnimation;
    }

    Animation getDefaultExitAnimation() {
        return defaultExitAnimation;
    }

    public Animation getDefaultPopEnterAnimation() {
        return defaultPopEnterAnimation;
    }

    public Animation getDefaultPopExitAnimation() {
        return defaultPopExitAnimation;
    }

    public Animation getDefaultReBuildEnterAnimation() {
        return defaultReBuildEnterAnimation;
    }

    public Animation getDefaultReBuildExitAnimation() {
        return defaultReBuildExitAnimation;
    }

    public File getSaveStateFile() {
        return saveStateFile;
    }

    public Cipher getSaveStateEncryptCipher() {
        return saveStateEncryptCipher;
    }

    public Cipher getSaveStateDecryptCipher() {
        return saveStateDecryptCipher;
    }

    /**
     * Set cipher used in save state
     *
     * @param encrypt initialized cipher use to encrypt
     * @param decrypt initialized cipher use to decrypt
     * @throws NullPointerException if either encrypt or decrypt cipher is null
     */
    public void setSaveStateCipher(Cipher encrypt, Cipher decrypt) {
        if (encrypt == null || decrypt == null) {
            throw new NullPointerException("Encrypt and Decrypt ciphers MUST NOT NULL");
        }
        saveStateEncryptCipher = encrypt;
        saveStateDecryptCipher = decrypt;
    }

    public static class Builder<ACT extends Activity, SV extends StatefulView> {
        private String initialRouteName;
        private Map<String, StatefulViewFactory<ACT, SV>> navMap;
        private Animation enterAnimation;
        private Animation exitAnimation;
        private Animation popEnterAnimation;
        private Animation popExitAnimation;
        private Animation reBuildEnterAnimation;
        private Animation reBuildExitAnimation;
        private File saveStateFile;
        private Cipher saveStateEncryptCipher;
        private Cipher saveStateDecryptCipher;

        /**
         * @param initialRouteName initial route to be pushed to navigator
         * @param navMap           mapping of the routes for this navigator
         */
        public Builder(String initialRouteName, Map<String, StatefulViewFactory<ACT, SV>> navMap) {
            this.initialRouteName = initialRouteName;
            this.navMap = navMap;
        }

        /**
         * Set default animation for this navigator
         *
         * @param enterAnimation    Animation when next view showing
         * @param exitAnimation     Animation when current view exiting
         * @param popEnterAnimation Animation when navigator pop and previous view showing
         * @param popExitAnimation  Animation when navigator pop and current view exiting
         */
        public Builder setAnimation(Animation enterAnimation, Animation exitAnimation, Animation popEnterAnimation, Animation popExitAnimation) {
            this.enterAnimation = enterAnimation;
            this.exitAnimation = exitAnimation;
            this.popEnterAnimation = popEnterAnimation;
            this.popExitAnimation = popExitAnimation;
            return this;
        }

        /**
         * Set default animation for this navigator when reBuildRoute is invoked
         *
         * @param enterAnimation Animation when next view showing
         * @param exitAnimation  Animation when current view exiting
         */
        public Builder setReBuildAnimation(Animation enterAnimation, Animation exitAnimation) {
            this.reBuildEnterAnimation = enterAnimation;
            this.reBuildExitAnimation = exitAnimation;
            return this;
        }

        /**
         * Provide file to save state, this file will be re-created and deleted as necessary.
         * <p>
         * the StatefulView states will be stored in this file by relying on java object serialization mechanism.
         * Use SealedObject class instead of default Serializable fields if you need to secure/encrypt them.
         * Or set the cipher {@link #setSaveStateCipher(Cipher, Cipher)} to automatically encrypt navigation state
         * <p>
         * When app gets killed and re-opened, navigator will handle state restoration,
         * see https://developer.android.com/topic/libraries/architecture/saving-states
         * This saving states behave the same as Saved instance state option.
         * The states will be cleared only when activity is finishing properly.
         * <p>
         * NOTE: Make sure you have decent java serialization knowledge before using this.
         * Saving state can be quiet tricky to handle.
         */
        public Builder setSaveStateFile(File file) {
            this.saveStateFile = file;
            return this;
        }

        /**
         * Encrypt navigation state cipher.
         * Encryption will not happen if either or both is null
         *
         * @param encrypt cipher to be used to encrypt, make sure it was initialized before set
         * @param decrypt cipher to be used to decryot, make sure it was initialized before set
         */
        public Builder setSaveStateCipher(Cipher encrypt, Cipher decrypt) {
            this.saveStateEncryptCipher = encrypt;
            this.saveStateDecryptCipher = decrypt;
            return this;
        }

        public NavConfiguration<ACT, SV> build() {
            NavConfiguration<ACT, SV> navConfiguration = new NavConfiguration<>(initialRouteName, navMap);
            if (enterAnimation == null) {
                AnimationSet inAnimationSet = new AnimationSet(true);
                inAnimationSet.setInterpolator(new DecelerateInterpolator());
                inAnimationSet.setDuration(200);
                inAnimationSet.addAnimation(new AlphaAnimation(0, 1));
                inAnimationSet.addAnimation(new TranslateAnimation(0, 0, 100, 0));
                enterAnimation = inAnimationSet;
            }
            if (exitAnimation == null) {
                AnimationSet outAnimationSet = new AnimationSet(true);
                outAnimationSet.setInterpolator(new LinearInterpolator());
                outAnimationSet.setDuration(200);
                outAnimationSet.addAnimation(new AlphaAnimation(0.5f, 0));
                exitAnimation = outAnimationSet;
            }
            if (popEnterAnimation == null) {
                AnimationSet inAnimationSet = new AnimationSet(true);
                inAnimationSet.setInterpolator(new LinearInterpolator());
                inAnimationSet.setDuration(200);
                inAnimationSet.addAnimation(new AlphaAnimation(0, 1));
                popEnterAnimation = inAnimationSet;
            }
            if (popExitAnimation == null) {
                AnimationSet outAnimationSet = new AnimationSet(true);
                outAnimationSet.setInterpolator(new LinearInterpolator());
                outAnimationSet.setDuration(200);
                outAnimationSet.addAnimation(new AlphaAnimation(0.5f, 0));
                outAnimationSet.addAnimation(new TranslateAnimation(0, 0, 0, 100));
                popExitAnimation = outAnimationSet;
            }
            if (reBuildEnterAnimation == null) {
                Animation inAnimation = new AlphaAnimation(0, 1);
                inAnimation.setDuration(200);
                reBuildEnterAnimation = inAnimation;
            }
            if (reBuildExitAnimation == null) {
                Animation outAnimation = new AlphaAnimation(1, 0);
                outAnimation.setDuration(200);
                reBuildExitAnimation = outAnimation;
            }
            navConfiguration.defaultEnterAnimation = enterAnimation;
            navConfiguration.defaultExitAnimation = exitAnimation;
            navConfiguration.defaultPopEnterAnimation = popEnterAnimation;
            navConfiguration.defaultPopExitAnimation = popExitAnimation;
            navConfiguration.defaultReBuildEnterAnimation = reBuildEnterAnimation;
            navConfiguration.defaultReBuildExitAnimation = reBuildExitAnimation;
            navConfiguration.saveStateFile = saveStateFile;
            if (saveStateEncryptCipher == null || saveStateDecryptCipher == null) {
                navConfiguration.saveStateEncryptCipher = new NullCipher();
                navConfiguration.saveStateDecryptCipher = new NullCipher();
            } else {
                navConfiguration.saveStateEncryptCipher = saveStateEncryptCipher;
                navConfiguration.saveStateDecryptCipher = saveStateDecryptCipher;
            }
            return navConfiguration;
        }
    }
}
