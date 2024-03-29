package m.co.rh.id.anavigator;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RouteOptions implements Externalizable {
    /**
     * Helper method to setup in and out animation for transition.
     *
     * @param enterAnimationResId    enter animation resource id to be inflated using AnimationUtils.loadAnimation,
     *                               if null means no animation
     * @param exitAnimationResId     exit animation resource id to be inflated using AnimationUtils.loadAnimation,
     *                               if null means no animation
     * @param popEnterAnimationResId pop enter animation resource id to be inflated using AnimationUtils.loadAnimation,
     *                               if null means no animation
     * @param popExitAnimationResId  pop exit animation resource id to be inflated using AnimationUtils.loadAnimation,
     *                               if null means no animation
     * @return RouteOptions instance with in and out animation setup
     */
    public static RouteOptions withAnimation(Integer enterAnimationResId, Integer exitAnimationResId,
                                             Integer popEnterAnimationResId, Integer popExitAnimationResId) {
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.enterAnimationResId = enterAnimationResId;
        routeOptions.exitAnimationResId = exitAnimationResId;
        routeOptions.popEnterAnimationResId = popEnterAnimationResId;
        routeOptions.popExitAnimationResId = popExitAnimationResId;
        return routeOptions;
    }

    /**
     * Helper method to setup enter and exit transition (using transition framework)
     *
     * @param enterTransitionResId enter transition resource id to be inflated using TransitionInflater.inflateTransition,
     *                             if null means no transition
     * @param exitTransitionResId  exit transition resource id to be inflated using TransitionInflater.inflateTransition,
     *                             if null means no transition
     * @return RouteOptions instance with enter and exit using transition framework setup
     */
    public static RouteOptions withTransition(Integer enterTransitionResId, Integer exitTransitionResId) {
        TransitionRouteOptions routeOptions = new TransitionRouteOptions();
        routeOptions.enterTransitionResId = enterTransitionResId;
        routeOptions.exitTransitionResId = exitTransitionResId;
        return routeOptions;
    }

    private Integer enterAnimationResId;
    private Integer exitAnimationResId;
    private Integer popEnterAnimationResId;
    private Integer popExitAnimationResId;

    public Integer getEnterAnimationResId() {
        return enterAnimationResId;
    }

    public Integer getExitAnimationResId() {
        return exitAnimationResId;
    }

    public Integer getPopEnterAnimationResId() {
        return popEnterAnimationResId;
    }

    public Integer getPopExitAnimationResId() {
        return popExitAnimationResId;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        int nullInt = -1;
        if (enterAnimationResId != null) {
            out.writeInt(enterAnimationResId);
        } else {
            out.writeInt(nullInt);
        }
        if (exitAnimationResId != null) {
            out.writeInt(exitAnimationResId);
        } else {
            out.writeInt(nullInt);
        }
        if (popEnterAnimationResId != null) {
            out.writeInt(popEnterAnimationResId);
        } else {
            out.writeInt(nullInt);
        }
        if (popExitAnimationResId != null) {
            out.writeInt(popExitAnimationResId);
        } else {
            out.writeInt(nullInt);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        int nullInt = -1;
        int tempIn = in.readInt();
        if (tempIn != nullInt) {
            enterAnimationResId = tempIn;
        }
        tempIn = in.readInt();
        if (tempIn != nullInt) {
            exitAnimationResId = tempIn;
        }
        tempIn = in.readInt();
        if (tempIn != nullInt) {
            popEnterAnimationResId = tempIn;
        }
        tempIn = in.readInt();
        if (tempIn != nullInt) {
            popExitAnimationResId = tempIn;
        }
    }
}

class TransitionRouteOptions extends RouteOptions {
    Integer enterTransitionResId;
    Integer exitTransitionResId;

    public TransitionRouteOptions() {
        super();
    }

    public Integer getEnterTransitionResId() {
        return enterTransitionResId;
    }

    public Integer getExitTransitionResId() {
        return exitTransitionResId;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        int nullInt = -1;
        if (enterTransitionResId != null) {
            out.writeInt(enterTransitionResId);
        } else {
            out.writeInt(nullInt);
        }
        if (exitTransitionResId != null) {
            out.writeInt(exitTransitionResId);
        } else {
            out.writeInt(nullInt);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        super.readExternal(in);
        int nullInt = -1;
        int tempIn = in.readInt();
        if (tempIn != nullInt) {
            enterTransitionResId = tempIn;
        }
        tempIn = in.readInt();
        if (tempIn != nullInt) {
            exitTransitionResId = tempIn;
        }
    }
}