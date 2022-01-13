package m.co.rh.id.anavigator.example.extension;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import m.co.rh.id.anavigator.StatefulView;
import m.co.rh.id.anavigator.annotation.NavInject;
import m.co.rh.id.anavigator.component.INavigator;
import m.co.rh.id.anavigator.example.MyApplication;
import m.co.rh.id.anavigator.example.R;
import m.co.rh.id.anavigator.extension.dialog.ui.NavExtDialogConfig;

public class ExtensionHomePage extends StatefulView<Activity> {

    @NavInject
    private transient INavigator mNavigator;

    @Override
    protected View createView(Activity activity, ViewGroup container) {
        NavExtDialogConfig navExtDialogConfig = MyApplication.of(activity).getNavExtDialogConfig();
        View view = activity.getLayoutInflater().inflate(R.layout.page_extension_home, container, false);
        Button showMessageDialogButton = view.findViewById(R.id.button_show_message_dialog);
        showMessageDialogButton.setOnClickListener(v -> {
            String exampleTitle = "This is title";
            String exampleContent = "this is content";
            mNavigator.push(navExtDialogConfig.getRoutePath(NavExtDialogConfig.ROUTE_MESSAGE),
                    navExtDialogConfig.args_messageDialog(exampleTitle, exampleContent));
        });
        return view;
    }
}
