# Rules for the app (release) APK only. The library consumer-rules.pro under test are merged in
# automatically from navigator/consumer-rules.pro and extension-dialog/consumer-rules.pro via
# consumerProguardFiles.
#
# This app intentionally does NOT blanket-keep the library (m.co.rh.id.anavigator.**):
# the whole point of this harness is to let R8 shrink/obfuscate the library exactly like
# it would in a real consumer app, with only the shipped consumer rules protecting the
# reflection-based annotation injection.

# SmokeResult is the volatile result holder read from the test APK
# (R8SmokeTest asserts its flags). R8 only sees writes from the app side, so
# without an explicit keep the unused-read static fields could be removed,
# which would surface as NoSuchFieldError in the test APK after remapping.
-keep class m.co.rh.id.anavigator.r8smoke.SmokeResult { *; }

# The androidTest APK IS minified by AGP 8.2 (its test-rules.pro restate AGP's
# injected defaults for robustness), and classes whose Maven
# coordinates also exist in the app APK (androidx.tracing, kotlin-stdlib) are
# deduped OUT of the test APK, so androidx.test:runner/monitor resolve them
# from the MINIFIED app APK at runtime. Without these keeps R8 merges both
# away in the app APK and AndroidJUnitRunner.onCreate crashes with
# NoClassDefFoundError before any test can run. (Same reasoning as the
# kotlin.** keep in the a-poi-spreadsheet harness.)
-keep class androidx.tracing.Trace { *; }
-keep class kotlin.** { *; }

