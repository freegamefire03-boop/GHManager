# Keep Gradle from stripping these
-keepattributes *Annotation*

# Keep Compose/AndroidX lifecycle-runtime-compose CompositionLocals (e.g.
# LocalLifecycleOwner) that collectAsStateWithLifecycle depends on. R8 otherwise
# strips the static CompositionLocal fields, crashing the app on launch with
# "CompositionLocal LocalLifecycleOwner not present".
-keep class androidx.lifecycle.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.platform.*CompositionLocals* { *; }
-keepclassmembers class androidx.compose.runtime.** {
    *;
}

# Keep Kotlin coroutines/flow intrinsics used by Compose state collection.
-keep class kotlinx.coroutines.flow.** { *; }
-keep class kotlinx.coroutines.** { *; }
