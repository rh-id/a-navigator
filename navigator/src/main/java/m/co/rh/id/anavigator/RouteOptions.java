package m.co.rh.id.anavigator;

import java.io.Serializable;

public class RouteOptions implements Serializable {
    /**
     * Helper method to setup in and out animation for transition.
     *
     * @param inAnimationResId  in animation resource id to be inflated using AnimationUtils.loadAnimation,
     *                          if null means no animation
     * @param outAnimationResId out animation resource id to be inflated using AnimationUtils.loadAnimation,
     *                          if null means no animation
     * @return RouteOptions instance with in and out animation setup
     */
    public static RouteOptions inOutAnimation(Integer inAnimationResId, Integer outAnimationResId) {
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setInAnimationResId(inAnimationResId);
        routeOptions.setOutAnimationResId(outAnimationResId);
        return routeOptions;
    }

    private Integer inAnimationResId;
    private Integer outAnimationResId;

    public Integer getInAnimationResId() {
        return inAnimationResId;
    }

    public void setInAnimationResId(Integer inAnimationResId) {
        this.inAnimationResId = inAnimationResId;
    }

    public Integer getOutAnimationResId() {
        return outAnimationResId;
    }

    public void setOutAnimationResId(Integer outAnimationResId) {
        this.outAnimationResId = outAnimationResId;
    }
}
