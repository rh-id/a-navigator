package m.co.rh.id.anavigator.extension.dialog.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.Serializable;

import m.co.rh.id.anavigator.NavRoute;
import m.co.rh.id.anavigator.StatefulViewDialog;
import m.co.rh.id.anavigator.component.RequireNavRoute;
import m.co.rh.id.anavigator.extension.dialog.R;

class ConfirmSVDialog extends StatefulViewDialog<Activity> implements RequireNavRoute, View.OnClickListener {

    private transient NavRoute mNavRoute;

    @Override
    public void provideNavRoute(NavRoute navRoute) {
        mNavRoute = navRoute;
    }

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        ViewGroup rootLayout = (ViewGroup) activity.getLayoutInflater()
                .inflate(R.layout.a_navigator_extension_dialog_confirm_dialog, container, false);
        TextView textTitle = rootLayout.findViewById(R.id.text_title);
        TextView textContent = rootLayout.findViewById(R.id.text_content);
        textTitle.setText(getTitle());
        textContent.setText(getContent());
        Button buttonOk = rootLayout.findViewById(R.id.button_ok);
        buttonOk.setOnClickListener(this);
        Button buttonCancel = rootLayout.findViewById(R.id.button_cancel);
        buttonCancel.setOnClickListener(this);
        return rootLayout;
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.button_ok) {
            getNavigator().pop(Result.withResult(true));
        } else if (viewId == R.id.button_cancel) {
            getNavigator().pop(Result.withResult(false));
        }
    }

    public String getTitle() {
        Args args = Args.of(mNavRoute);
        if (args != null) {
            return args.mTitle;
        }
        return null;
    }

    public String getContent() {
        Args args = Args.of(mNavRoute);
        if (args != null) {
            return args.mContent;
        }
        return null;
    }

    static class Result implements Serializable {
        public static Result withResult(Boolean result) {
            Result result1 = new Result();
            result1.mConfirmed = result;
            return result1;
        }

        public static Result of(NavRoute navRoute) {
            if (navRoute != null) {
                return of(navRoute.getRouteResult());
            }
            return null;
        }

        public static Result of(Serializable serializable) {
            if (serializable instanceof Result) {
                return (Result) serializable;
            }
            return null;
        }

        private Boolean mConfirmed;

        public Boolean isConfirmed() {
            return mConfirmed;
        }
    }

    static class Args implements Serializable {
        public static Args newArgs(String title, String content) {
            Args args = new Args();
            args.mTitle = title;
            args.mContent = content;
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

        private String mTitle;
        private String mContent;
    }
}
