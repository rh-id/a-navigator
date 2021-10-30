# a-navigator

![Android CI](https://github.com/rh-id/a-navigator/actions/workflows/gradlew-build.yml/badge.svg)

This is a navigation framework for Android projects.
This framework doesn't follow common Model View View Model (MVVM) approach.
It follows what in my opinion called Model Stateful View (MSV) approach.
In MVVM pattern, ViewModel hold the state and "glue" both UI and business logic, and Activity/Fragment handles View and lifecycle logic.
In MSV pattern, StatefulView hold state, "glue", View creation, and Lifecycle when necessary.

To put it simply, imagine Fragment and ViewModel as one component, a fragment that hold its state AND with an easy to use navigator.
One navigator controls one activity.

Nested navigator supported through `INavigator.createViewNavigator`.
you will need to have one navigator as root and setup view navigator by calling that method.

This navigator can handle Jetpack Compose View as well,
see `AppCompatExampleActivity.java` and `ExampleComposePage.kt` for example implementation.
It is best to use Java language rather than Kotlin when using this library especially when save state is enabled.
If you are not planning to use save state then it should be fine.

## Example Usage

This project support jitpack, in order to use this, you need to add jitpack to your project root build.gradle:
```
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
        jcenter() // Warning: this repository is going to shut down soon
    }
}
```

Include this to your module dependency (module build.gradle)
```
dependencies {
    implementation 'com.github.rh-id:a-navigator:v0.0.1'
}
```

Next for code part, create a home page by extending `StatefulView` (see example package in example folder)

```
public class HomePage extends StatefulView<Activity> {

    @Override
    protected void initState(Activity activity) {
        // init your state here
    }

    @Override
    protected View createView(Activity activity) {
        // inflate and setup your view here
        View view = activity.getLayoutInflater().inflate(R.layout.page_home, null, false);
        return view;
    }

    @Override
    public void dispose(Activity activity) {
        // do cleanup when navigator pop this StatefulView from stack
    }
}
```

Initialize on your application for global access

```
public class MyApplication extends Application {

    private Navigator<MainActivity, StatefulView<Activity>>
                mainActivityNavigator;

    @Override
    public void onCreate() {
        super.onCreate();
        Map<String, StatefulViewFactory<MainActivity, StatefulView<Activity>>> navMap = new HashMap<>();
        // this is where you map all your StatefulView implementations
        navMap.put("/", (args, activity) -> new HomePage());

        // make sure to set initial route to home page which is "/"
        NavConfiguration.Builder<MainActivity, StatefulView<Activity>> navBuilder = new NavConfiguration.Builder<>("/", navMap);

        // set File to save state if you want navigator to save its state
        navBuilder.setSaveStateFile(new File(getCacheDir(), "navigator1State"));

        NavConfiguration<MainActivity, StatefulView<Activity>> navConfiguration =
                navBuilder.build();
        mainActivityNavigator =
                new Navigator<>(MainActivity.class, navConfiguration);

        // make sure to register navigator as callbacks to work properly
        registerActivityLifecycleCallbacks(mainActivityNavigator);
        registerComponentCallbacks(mainActivityNavigator);

        // Extra example setup if you have nested navigator, for example when using bottom navigation
        Map<String, StatefulViewFactory<RawActivity, StatefulView<Activity>>> bottomPageMap = new HashMap<>();
        bottomPageMap.put("/", (args, activity) -> new BottomHomePage());
        bottomPageMap.put("/page1", (args, activity) -> new Bottom1Page());
        bottomPageMap.put("/page2", (args, activity) -> new Bottom2Page());
        NavConfiguration.Builder<RawActivity, StatefulView<Activity>> navBuilderBottom = new NavConfiguration.Builder<>("/", bottomPageMap);
        // you could also enable save state for this nested/view navigator
        navBuilderBottom.setSaveStateFile(new File(getCacheDir(), "navigatorBottomState"));
        navigator.createViewNavigator(navBuilderBottom.build(), R.id.unique_container1);

    }

    public Navigator
        getNavigator(Activity activity) {
            if (activity instanceof MainActivity) {
                return mainActivityNavigator;
            }
            return null;
        }
}
```

Configure your main activity to allow the navigator to listen when back button is pressed

```
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();
        // this is required to let navigator handle the back button
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                MyApplication.of(MainActivity.this)
                        .getNavigator(MainActivity.this).onBackPressed();
            }
        });
    }
}
```

If you are not extending `AppCompatActivity` you need to configure it like this:

```
/**
 * Example to use the framework by extending Activity directly
 */
public class MainActivity extends Activity {

    @Override
    public void onBackPressed() {
        // this is required to let navigator handle the back button
        MyApplication.of(this).getNavigator(this).onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // this is required to let navigator handle onActivityResult
        MyApplication.of(this).getNavigator(this).onActivityResult(requestCode, resultCode, data);
    }
}
```
