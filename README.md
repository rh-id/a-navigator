# a-navigator

![JitPack](https://img.shields.io/jitpack/v/github/rh-id/a-navigator)
![Downloads](https://jitpack.io/v/rh-id/a-navigator/week.svg)
![Downloads](https://jitpack.io/v/rh-id/a-navigator/month.svg)
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

The StatefulViews and its stack can survive process death by enabling save state capabilities in the navigator configuration.
When save state is enabled, any fields that are serializable and not marked as `transient` will be saved into the save state file following java object serialization standard.
Because of this you do not need to think on how to save and restore state using saveStateInstance bundle as long as java object serialization standards are followed.

## Example Usage

For example usage ![see project example](https://github.com/rh-id/a-navigator/tree/master/example), for example production app see ![a-news-provider](https://github.com/rh-id/a-news-provider)

This project support jitpack, in order to use this, you need to add jitpack to your project root build.gradle:
```
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
    }
}
```

Include this to your module dependency (module build.gradle)
```
dependencies {
    // this will include navigator module and all its extension module
    implementation 'com.github.rh-id:a-navigator:v0.0.1'
    
    // use these if you want the navigator with its extension separately
    implementation 'com.github.rh-id.a-navigator:a-navigator:v0.0.1'
    implementation 'com.github.rh-id.a-navigator:a-navigator-extension-dialog:v0.0.1'
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
This framework also support injection by annotating fields with `@NavInject`
```
public class HomePage extends StatefulView<Activity> {

    // navigator will be injected before initState is being called
    @NavInject
    private transient INavigator mNavigator;

    @NavInject
    private StatefulView mReuseStatefulView;
    // If save state is enabled, 
    // any serializable field will be saved to the file
    // and restored when deserialized
    private String mExampleStringField;

    // this can be injected when setting up NavConfiguration with the required component
    @NavInject
    private transient MyGlobalComponent mMyGlobalComponent;

    public HomePage(){
        // still need to instantiate StatefulView manually,
        // navigator will only inject if this StatefulView is not null
        mReuseStatefulView = new ReuseStatefulView();
    }

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
Injection by annotations is using reflection under the hood which might be slow.

If you find that navigator seemed to cause slowness, try to disable annotations injection and use `INavigator.injectRequired`,
to manually inject the StatefulViews.

NOTE:
It is better to just use this feature for convenience (Really, a HUGE convenience),
reflection performance might be slow but this framework mitigate it by processing reflection concurrently.
Measure it first before decide if the slowness really comes from this framework.
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

        // disable annotations functionality
        navBuilder.setEnableAnnotationInjection(false);
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
## Example Projects
<ul>
<li>https://github.com/rh-id/a-news-provider</li>
<li>https://github.com/rh-id/a-flash-deck</li>
<li>https://github.com/rh-id/a-medic-log</li>
</ul>

## Support this project
![Bitcoin](https://img.shields.io/badge/Bitcoin-000000?style=for-the-badge&logo=bitcoin&logoColor=white&link=bitcoin://bc1qk9n2kljqyunqvlpyjxd4f4tt2xl0uwt2ak9xu4)
bc1qk9n2kljqyunqvlpyjxd4f4tt2xl0uwt2ak9xu4
