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

# Keep Compose text + font resolution. R8 was optimizing away part of the text
# measurement path, causing a NullPointerException in
# androidx.compose.ui.text.platform.AndroidParagraphIntrinsics.<init> the moment
# any Text was measured inside the settings Dialog (release only; debug was fine).
-keep class androidx.compose.ui.text.** { *; }
-keepclassmembers class androidx.compose.ui.text.** { *; }
-keep class androidx.compose.ui.text.font.** { *; }
-keep class androidx.compose.ui.text.platform.** { *; }
-dontwarn androidx.compose.ui.text.**

# Emoji2 font provider (used by Compose text when measuring glyphs).
-keep class androidx.emoji2.** { *; }
-dontwarn androidx.emoji2.**

# R8's optimization pass was rewriting the Compose text-measurement code in a way
# that produced a NullPointerException in AndroidParagraphIntrinsics.<init>
# (release only; debug worked). Disable the optimization pass but keep shrinking
# + obfuscation so the APK stays small.
-dontoptimize
