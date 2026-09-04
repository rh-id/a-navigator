# a-navigator consumer rules.
# Applied automatically to consumer apps via consumerProguardFiles.
#
# Navigator.injectStatefulView injects dependencies into StatefulView fields
# annotated with @NavInject / @NavRouteIndex / @NavViewNavigator via runtime
# reflection (getDeclaredFields -> Field.getAnnotation -> setAccessible ->
# Field.set). That reflective access is invisible to R8: without these rules
# R8 strips the annotation from the fields (and may remove the fields
# themselves), injection silently no-ops, the fields stay null and the app
# crashes on first use of the injected dependency.

# --- Annotation classes read reflectively by Navigator ---
-keep class m.co.rh.id.anavigator.annotation.NavInject { *; }
-keep class m.co.rh.id.anavigator.annotation.NavRouteIndex { *; }
-keep class m.co.rh.id.anavigator.annotation.NavViewNavigator { *; }

# --- Annotated fields must survive shrinking, keeping their annotations ---
-keepclasseswithmembernames class * {
    @m.co.rh.id.anavigator.annotation.NavInject <fields>;
}
-keepclasseswithmembernames class * {
    @m.co.rh.id.anavigator.annotation.NavRouteIndex <fields>;
}
-keepclasseswithmembernames class * {
    @m.co.rh.id.anavigator.annotation.NavViewNavigator <fields>;
}
