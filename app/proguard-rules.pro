# ── Kotlin metadata ──────────────────────────────────────────────────────────
# Required so Kotlin reflection and inline functions work in release builds.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ── Android Telecom ───────────────────────────────────────────────────────────
# Telecom invokes Call.Callback subclasses and InCallService via reflection.
-keep class * extends android.telecom.Call$Callback { *; }
-keep class * extends android.telecom.InCallService { *; }
-keep class * extends android.telecom.CallScreeningService { *; }

# ── Accessibility service ─────────────────────────────────────────────────────
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

# ── Coil ─────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── Lifecycle / Compose lifecycle ─────────────────────────────────────────────
# lifecycle-runtime-compose 2.8.x moves LocalLifecycleOwner; R8 can strip the
# composition-local provider, causing "LocalLifecycleOwner not present" at runtime.
-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }

# ── Compose composition locals ────────────────────────────────────────────────
# ViewTreeLifecycleOwner provides LocalLifecycleOwner to AbstractComposeView.
-keep class androidx.compose.ui.platform.** { *; }

# Note: Activities, Services, and BroadcastReceivers listed in AndroidManifest.xml
# are automatically kept by proguard-android-optimize.txt (the base file).
