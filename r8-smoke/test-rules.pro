# Rules for the androidTest (release) APK - applied when AGP minifies the test APK
# (AGP does so on all supported versions, including 8.2 - see
# build/outputs/mapping/releaseAndroidTest/mapping.txt).
# Keep every test-APK class (AGP injects equivalent rules for minified test APKs; restated here for robustness - harmless duplication).
-keep,allowobfuscation class ** { *; }
# Keep the harness package names in the test APK so test classes stay resolvable against the app APK's mapping.
-keeppackagenames m.co.rh.id.anavigator.r8smoke.**
# androidx.test:monitor's Kotlin lambdas can crash with NoClassDefFoundError kotlin.jvm.internal.Lambda
# after R8 horizontal class merging; keep the stdlib.
-keep class kotlin.** { *; }
# androidx.test:monitor Tracer$Span references compile-only errorprone annotations.
-dontwarn com.google.errorprone.annotations.MustBeClosed
