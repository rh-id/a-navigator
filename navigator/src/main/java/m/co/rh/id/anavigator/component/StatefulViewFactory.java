package m.co.rh.id.anavigator.component;

import android.app.Activity;

import m.co.rh.id.anavigator.StatefulView;

/**
 * A factory to create StatefulView instance
 */
public interface StatefulViewFactory<ACT extends Activity, SV extends StatefulView> {
    /**
     * Creates new StatefulView
     */
    SV newInstance(Object args, ACT activity);
}
