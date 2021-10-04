package m.co.rh.id.anavigator.component;

import android.app.Activity;

import java.io.Serializable;

import m.co.rh.id.anavigator.StatefulView;

/**
 * A factory to create StatefulView instance
 */
public interface StatefulViewFactory<ACT extends Activity, SV extends StatefulView> extends Serializable {
    /**
     * Creates new StatefulView
     */
    SV newInstance(Serializable args, ACT activity);
}
