package m.co.rh.id.anavigator;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;

import java.io.Serializable;

import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.RequireNavigator;

/**
 * StatefulView implementation to handle dialog state, extends this class if custom dialog is needed.
 * Override {@link #createView(Activity, ViewGroup)} if only custom content view of dialog is needed.
 * Override {@link #createDialog(Activity)} if custom dialog is needed,
 * no need to override {@link #createView(Activity, ViewGroup)} if you decide to override this.
 */
public class StatefulViewDialog<ACT extends Activity> extends StatefulView<ACT>
        implements RequireNavigator,
        DialogInterface.OnDismissListener, DialogInterface.OnCancelListener,
        DialogInterface.OnShowListener {

    private transient INavigator mNavigator;
    private transient Dialog mActiveDialog;
    private transient boolean mShouldPop;

    /**
     * Convenience constructor if you are going to use this dialog as a route.
     * For re-use case, inject it using @NavInject or call provideNavigator manually
     */
    public StatefulViewDialog() {
        this(null);
    }

    public StatefulViewDialog(INavigator navigator) {
        mNavigator = navigator;
    }

    @Override
    public void provideNavigator(INavigator navigator) {
        mNavigator = navigator;
    }

    @Override
    protected View createView(ACT activity, ViewGroup container) {
        return null; // return null to use default dialog content view
    }

    /**
     * create default dialog with content view from {@link #createView(Activity, ViewGroup)}
     * Override this to create custom dialog.
     * <p>
     * The resulting dialog Dialog.setOnDismissListener, Dialog.setOnCancelListener, Dialog.setOnShowListener
     * will be replaced with this class implementation.
     * <p>
     * override {@link #onDismissDialog(DialogInterface)} to handle Dialog.setOnDismissListener.
     * override {@link #onCancelDialog(DialogInterface)} to handle Dialog.setOnCancelListener.
     * override {@link #onShowDialog(DialogInterface)} to handle Dialog.setOnShowListener
     */
    protected Dialog createDialog(ACT activity) {
        Dialog dialog = new Dialog(activity);
        View contentView = buildView(activity, null);
        if (contentView != null) {
            dialog.setContentView(contentView);
        }
        return dialog;
    }

    final Dialog initDialog(ACT activity) {
        initialize(activity);
        if (mActiveDialog == null) {
            mActiveDialog = createDialog(activity);
            mActiveDialog.setOnDismissListener(this);
            mActiveDialog.setOnCancelListener(this);
            mActiveDialog.setOnShowListener(this);
        }
        return mActiveDialog;
    }

    final void showDialog(ACT activity) {
        Dialog dialog = initDialog(activity);
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    final void dismissWithoutPop(ACT activity) {
        mShouldPop = false;
        Dialog dialog = initDialog(activity);
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        mActiveDialog = null;
    }

    @Override
    public void dispose(ACT activity) {
        super.dispose(activity);
        if (mActiveDialog != null) {
            mShouldPop = false;
            mActiveDialog.dismiss();
        }
        mActiveDialog = null;
        mNavigator = null;
    }

    protected void onShowDialog(DialogInterface dialog) {
        // leave blank
    }

    protected void onDismissDialog(DialogInterface dialog) {
        // leave blank
    }

    protected void onCancelDialog(DialogInterface dialog) {
        // leave blank
    }

    /**
     * Dialog result to be passed when a dialog was dismissed outside user control.
     * This result will be passed to navigator.pop().
     * This is useful for AlertDialog where cancel and ok button directly dismiss the dialog,
     * in which you must NOT trigger navigator.pop() since it is done automatically when dialog is dismissed
     */
    protected Serializable getDialogResult() {
        return null;
    }

    protected INavigator getNavigator() {
        return mNavigator;
    }

    @Override
    public final void onCancel(DialogInterface dialog) {
        onCancelDialog(dialog);
    }

    @Override
    public final void onDismiss(DialogInterface dialog) {
        if (mShouldPop) {
            mNavigator.pop(getDialogResult());
        }
        onDismissDialog(dialog);
    }

    @Override
    public final void onShow(DialogInterface dialog) {
        mShouldPop = true;
        onShowDialog(dialog);
    }
}
