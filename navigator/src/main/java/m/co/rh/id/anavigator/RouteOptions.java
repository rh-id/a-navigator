package m.co.rh.id.anavigator;

import java.io.Serializable;

public class RouteOptions implements Serializable {
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
}
