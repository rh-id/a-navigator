package m.co.rh.id.anavigator.extension.dialog.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.Serializable;

import m.co.rh.id.anavigator.NavRoute;
import m.co.rh.id.anavigator.StatefulViewDialog;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.component.NavBackPressResult;
import m.co.rh.id.anavigator.component.NavOnBackPressed;
import m.co.rh.id.anavigator.component.RequireNavRoute;
import m.co.rh.id.anavigator.extension.dialog.R;

/**
 * Common non-cancellable progress dialog with title, message and a
 * horizontal progress line.
 * <p>
 * This class is <b>public</b> on purpose, deviating from the package-private
 * convention of the other dialogs in this module: the caller must be able to
 * hold the typed {@link Args} reference to stream live updates
 * ({@code updateMessage}/{@code updateProgress}) while the dialog is on
 * screen, and {@link Args} is a member of this class - JLS 6.6.1 requires the
 * enclosing class itself to be accessible for its member types to be.
 * <p>
 * <b>Non-cancellable semantics (both back paths):</b>
 * <ul>
 *     <li>the navigator path: the dialog implements
 *     {@link NavOnBackPressed} and always returns
 *     {@link NavBackPressResult#noPop()}, so a back press delivered through
 *     the navigator never pops the dialog route</li>
 *     <li>the dialog-window path: {@link #createDialog(Activity)} sets
 *     {@code setCancelable(false)} and
 *     {@code setCanceledOnTouchOutside(false)}, so touching outside or the
 *     dialog-window back handling never dismisses the window either</li>
 * </ul>
 * <b>Caller-pops contract:</b> the dialog has NO result and NEVER pops
 * itself. The caller is responsible for popping it (typically
 * {@code navigator.pop()} from a {@code doFinally} block) when its async
 * work completes, regardless of success or failure.
 * <p>
 * <b>Pushing another route:</b> pushing any route while this dialog is on
 * top dismisses the dialog window WITHOUT popping the dialog route
 * ({@code Navigator.checkAndDismissDialog}); when the new route is popped
 * the progress dialog re-appears. The caller must either re-push on top or
 * account for this behavior in its flow.
 * <p>
 * <b>Dismiss/re-show behavior:</b> {@code dismissWithoutPop} is final in
 * {@link StatefulViewDialog}, so the {@link Args} update listener registered
 * by this dialog persists across a dismiss followed by a re-show. Both
 * re-registration paths are handled: when the view is re-created,
 * {@link #createView(Activity, ViewGroup)} registers a fresh listener on the
 * same {@link Args} instance, and a posted-but-not-yet-run update runnable
 * is guarded by the view fields still being non-null so a disposed dialog
 * never touches dead views.
 * <p>
 * <b>Theming:</b> the progress bar intentionally carries NO tint attribute,
 * it follows the host app theme. Theme-following tinting of framework
 * widgets requires API 21+ while this module's minSdk is 14, so older
 * devices simply see the platform default styling.
 * <p>
 * The state is held in the {@link Args} instance passed as the route
 * argument: it is serialized by the navigator's snapshot (so the latest
 * title/message/progress survive process death), while the update listener
 * on {@link Args} is transient (the navigator's SnapshotHandler would fail
 * to save a non-serializable listener and silently skip the whole snapshot,
 * losing state persistence).
 */
public class ProgressSVDialog extends StatefulViewDialog<Activity> implements RequireNavRoute, NavOnBackPressed<Activity> {

    private transient NavRoute mNavRoute;
    private transient TextView mTextTitle;
    private transient TextView mTextMessage;
    private transient ProgressBar mProgressBar;
    private transient Handler mMainHandler;

    @Override
    public void provideNavRoute(NavRoute navRoute) {
        mNavRoute = navRoute;
    }

    @Override
    public NavBackPressResult onBackPressed(View currentView, Activity activity, INavigator navigator) {
        // back must not dismiss the progress dialog - this covers the
        // navigator path, setCancelable(false) in createDialog covers the
        // dialog-window path
        return NavBackPressResult.noPop();
    }

    @Override
    protected Dialog createDialog(Activity activity) {
        Dialog dialog = super.createDialog(activity);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        ViewGroup rootLayout = (ViewGroup) activity.getLayoutInflater()
                .inflate(R.layout.a_navigator_extension_dialog_progress_dialog, container, false);
        mTextTitle = rootLayout.findViewById(R.id.text_title);
        mTextMessage = rootLayout.findViewById(R.id.text_message);
        mProgressBar = rootLayout.findViewById(R.id.progress_bar);
        mMainHandler = new Handler(Looper.getMainLooper());
        Args args = Args.of(mNavRoute);
        if (args != null) {
            // register the transient update callback FIRST: any update
            // arriving between registration and the initial render below is
            // re-read under the Args monitor and posted to the main thread,
            // so no notification can be lost in this window
            args.setUpdateListener(() -> {
                // re-read the latest conflated state under the Args monitor
                String title = args.getTitle();
                String message = args.getMessage();
                Integer max = args.getMax();
                int progress = args.getProgress();
                mMainHandler.post(() -> {
                    // guard: dispose() may have run between post and run,
                    // never apply state to dead views
                    if (mTextTitle == null || mTextMessage == null || mProgressBar == null) {
                        return;
                    }
                    applyArgs(title, message, max, progress);
                });
            });
            // THEN read the state under the monitor via the synchronized
            // getters for the initial render
            String title = args.getTitle();
            String message = args.getMessage();
            Integer max = args.getMax();
            applyArgs(title, message, max, args.getProgress());
        }
        return rootLayout;
    }

    /**
     * Applies the provided state to the current view fields. ProgressBar
     * clamps values internally, so setting progress before setMax is safe;
     * the ordered indeterminate/max/progress sequence is kept anyway.
     *
     * @param title   title to show, null hides the title
     * @param message message to show, null hides the message
     * @param max     determinate max value, null keeps the bar indeterminate
     * @param progress current progress value (used when max is non-null)
     */
    private void applyArgs(String title, String message, Integer max, int progress) {
        if (title != null) {
            mTextTitle.setText(title);
            mTextTitle.setVisibility(View.VISIBLE);
        } else {
            mTextTitle.setVisibility(View.GONE);
        }
        if (message != null) {
            mTextMessage.setText(message);
            mTextMessage.setVisibility(View.VISIBLE);
        } else {
            mTextMessage.setVisibility(View.GONE);
        }
        if (max != null) {
            mProgressBar.setIndeterminate(false);
            mProgressBar.setMax(max);
            mProgressBar.setProgress(progress);
        }
        // else: leave the bar in its current (indeterminate) mode
    }

    @Override
    public void dispose(Activity activity) {
        // super FIRST: dialog dismissal + base-class listener detach happen
        // there
        super.dispose(activity);
        // detach the update listener so a disposed dialog no longer observes
        // the Args state, then clear the view fields
        Args args = Args.of(mNavRoute);
        if (args != null) {
            args.setUpdateListener(null);
        }
        mNavRoute = null;
        mTextMessage = null;
        mTextTitle = null;
        mProgressBar = null;
        mMainHandler = null;
    }

    /**
     * Arguments of the progress dialog, thread-safe: state can be updated
     * from ANY thread while the dialog is displayed. Updates are conflated
     * state (latest-value-wins), each update simply overwrites the previous
     * value and the dialog renders whatever is latest. The update listener
     * is transient and is (re-)registered by the dialog on view
     * (re)creation; the state itself survives process death via
     * serialization (the navigator's snapshot).
     */
    public static class Args implements Serializable {

        private String mTitle;
        private String mMessage;
        private Integer mMax;
        private int mProgress;
        private transient OnArgsUpdateListener mUpdateListener;

        /**
         * Creates arguments for an indeterminate progress dialog.
         *
         * @param title   title to be shown, null hides the title
         * @param message message to be shown, ignored when null
         * @return arguments, pass this as argument to the navigator
         */
        public static Args newArgs(String title, String message) {
            Args args = new Args();
            args.mTitle = title;
            if (message != null) {
                args.mMessage = message;
            }
            return args;
        }

        /**
         * Creates arguments for a determinate progress dialog
         * (when max is non-null), or an indeterminate one (when max is null).
         *
         * @param title   title to be shown, null hides the title
         * @param message message to be shown, ignored when null
         * @param max     maximum value of the progress bar, null keeps the
         *                bar indeterminate
         * @return arguments, pass this as argument to the navigator
         */
        public static Args newArgs(String title, String message, Integer max) {
            Args args = newArgs(title, message);
            args.mMax = max;
            return args;
        }

        public static Args of(NavRoute navRoute) {
            if (navRoute != null) {
                return of(navRoute.getRouteArgs());
            }
            return null;
        }

        public static Args of(Serializable serializable) {
            if (serializable instanceof Args) {
                return (Args) serializable;
            }
            return null;
        }

        public synchronized String getTitle() {
            return mTitle;
        }

        synchronized String getMessage() {
            return mMessage;
        }

        public synchronized Integer getMax() {
            return mMax;
        }

        synchronized int getProgress() {
            return mProgress;
        }

        /**
         * Updates the message (latest value wins) and notifies the dialog.
         *
         * @param message new message, null hides the message
         */
        public void updateMessage(String message) {
            OnArgsUpdateListener listener;
            synchronized (this) {
                mMessage = message;
                listener = mUpdateListener;
            }
            if (listener != null) {
                listener.onArgsUpdated();
            }
        }

        /**
         * Updates the progress value (latest value wins) and notifies the
         * dialog.
         *
         * @param progress new progress value, clamped by the ProgressBar
         */
        public void updateProgress(int progress) {
            OnArgsUpdateListener listener;
            synchronized (this) {
                mProgress = progress;
                listener = mUpdateListener;
            }
            if (listener != null) {
                listener.onArgsUpdated();
            }
        }

        /**
         * Registers the update listener (used by the dialog itself),
         * pass null to detach.
         *
         * @param l listener to notify on state change, may be null
         */
        synchronized void setUpdateListener(OnArgsUpdateListener l) {
            mUpdateListener = l;
        }

        /**
         * Listener notified when the Args state changed, NOT
         * {@link Runnable} so the semantics stay unambiguous (it is an args
         * update event, not a task to execute).
         */
        interface OnArgsUpdateListener {
            void onArgsUpdated();
        }
    }
}
