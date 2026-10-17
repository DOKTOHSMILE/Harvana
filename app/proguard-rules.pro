# R8 rules for the release build. The former blanket -ignorewarnings/-dontwarn/-dontnote
# were removed so that genuinely missing classes surface at build time instead of at runtime.
# Firebase, Play services, OkHttp and Gson ship their own consumer rules.

-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Gson TypeToken (used in TrustScoreEngine / HomeFragment) needs generic signatures preserved
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Fragments are re-created by the framework through their no-arg constructors
-keep public class * extends androidx.fragment.app.Fragment { public <init>(); }
-keep public class * extends androidx.fragment.app.DialogFragment { public <init>(); }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { **[] $VALUES; public *; }
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder { *** rewind(); }

# QR scanner library (JitPack)
-keep class com.budiyev.android.codescanner.** { *; }

# Staking reward chart
-keep class com.github.mikephil.charting.** { *; }

# BouncyCastle: only the pieces referenced directly are needed; it ships no reflection-based lookups here
-dontwarn org.bouncycastle.**

# ============================================================================
# XML-related keep rules
# ----------------------------------------------------------------------------
# Android instantiates many classes by name from XML attributes at inflate time.
# LayoutInflater, DrawableInflater, AnimatorInflater and TransitionInflater all
# do Class.forName(...) on the value of an XML attribute. R8's whole-program
# analysis cannot see XML as a use, so without these keeps the class is pruned
# and the app crashes in release builds with ClassNotFoundException at inflate
# time. These rules cover every category of XML-loaded class. AndroidX, Material,
# Firebase, OkHttp, Glide, MPAndroidChart, Gson and BouncyCastle ship their own
# consumer rules and have been verified to not need extra keeps beyond this list.
# ============================================================================

# 1. CoordinatorLayout Behavior and LayoutParams (loaded by name from
#    app:layout_behavior and app:layout_anchor / app:layout_anchorGravity).
-keep public class * extends androidx.coordinatorlayout.widget.CoordinatorLayout$Behavior {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keep public class * extends androidx.coordinatorlayout.widget.CoordinatorLayout$LayoutParams {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}
# Material's BottomSheetBehavior is the offender seen in production: it is only
# referenced by string from the material AAR's design_bottom_sheet_dialog layout,
# and the app's Java only touches BottomSheetDialog + BottomSheetBehavior.from(...).
# Keep the whole package as a belt-and-braces guard against any other layout
# that does the same trick.
-keep class com.google.android.material.bottomsheet.** { *; }

# 2. Custom View and ViewGroup subclasses inflated from layout XML. View needs
#    the (Context, AttributeSet) constructor; ViewGroup adds the int defStyleAttr
#    variant. The framework's two-arg constructor is the one LayoutInflater uses.
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keep public class * extends android.view.ViewGroup {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 3. Custom Drawable subclasses inflated from drawable/*.xml or referenced as
#    android:src / app:srcCompat. DrawableInflater uses the (Context, Resources,
#    XmlPullParser [, AttributeSet]) constructors.
-keep public class * extends android.graphics.drawable.Drawable {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.content.res.Resources);
    public <init>(android.content.Context, android.content.res.Resources, org.xmlpull.v1.XmlPullParser);
    public <init>(android.content.Context, android.content.res.Resources, org.xmlpull.v1.XmlPullParser, android.util.AttributeSet);
}

# 4. Custom Animation (anim/*.xml) and Animator (animator/*.xml) subclasses.
#    R8 prunes them if no Java code calls new on them; the inflater only does
#    Class.forName(...).
-keep public class * extends android.view.animation.Animation {
    public <init>();
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keep public class * extends android.animation.Animator {
    public <init>();
}

# 5. Custom Transition subclasses inflated from transition/*.xml.
-keep public class * extends android.transition.Transition {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keep public class * extends androidx.transition.Transition {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# 6. Methods called from android:onClick="methodName" in XML. R8 already keeps
#    Activity, Service and Fragment classes (rules elsewhere in this file) but
#    the public void (View) method itself must be kept by name so LayoutInflater
#    can find it by reflection. Same for android:onLongClick.
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
    public boolean *(android.view.View);
}
-keepclassmembers class * extends androidx.fragment.app.Fragment {
    public void *(android.view.View);
    public boolean *(android.view.View);
}
-keepclassmembers class * extends android.app.Service {
    public void *(android.view.View);
    public boolean *(android.view.View);
}

# 7. Styleable resources. Custom attributes declared with <declare-styleable>
#    in attrs.xml generate integer arrays in R$styleable. Context.obtainStyledAttributes
#    looks them up by reflection, which R8 cannot see as a reference, so the
#    field can be pruned and the attribute silently reads 0 at runtime.
-keepclassmembers class **.R$styleable { *; }
-keepclassmembers class **.R$attr { *; }

# Play services references this annotation; it is not in the app. A missing class here is a warning, not a crash.
-dontwarn com.google.android.apps.common.proguard.SideEffectFree

# Firebase is built with @AutoValue. The annotation processor is not in the runtime classpath,
# only the generated code is, so the annotation class itself is never on the device. R8 sees the
# references in StartupTime and 10 other Firebase classes; without this rule it logs a warning
# for every reference and, in some Firebase versions, turns it into a validation error.
-dontwarn com.google.auto.value.**
-dontwarn com.google.auto.value.extension.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.inject.**

# ============================================================================
# Firebase + google-services.json runtime resolution
# -----------------------------------------------------------------------------
# The error "An internal error has occurred. [API key not found. Please pass a valid API key.]"
# means R8 pruned one of the classes Firebase uses at runtime to read the API key
# out of google-services.json. The Firebase SDK uses reflection on three things
# during init:
#   (a) the FirebaseInitProvider ContentProvider declared in the merged manifest,
#   (b) the generated google-services resources (string resources injected by
#       the google-services Gradle plugin), and
#   (c) Firebase / Play services classes that look up the key on the classpath.
# Without explicit keeps R8 prunes (a) and (c) and the SDK then reports
# "API key not found" on the first call. The rules below keep all three.
# ============================================================================

# (a) FirebaseInitProvider is in the merged manifest. R8 prunes it because
#     the app's Java never references it directly; the keep here is the only
#     thing that keeps the class on the device.
-keep class com.google.firebase.provider.FirebaseInitProvider { *; }

# (b) The google-services Gradle plugin generates string resources whose values
#     are read at runtime by FirebaseOptions.fromResource(). Keep every R$* member
#     so the generated google_app_id, default_web_client_id, etc. survive R8.
-keepclassmembers class **.R$* {
    public static <fields>;
}
# Same for the styleable and attr classes Firebase may read indirectly.
-keepclassmembers class **.R$styleable { *; }
-keepclassmembers class **.R$attr { *; }

# (c) Keep every Firebase and Play services class. They use reflection to wire
#     providers, auth, firestore, storage, database, appcheck, crashlytics and
#     the messaging FCM endpoint. A blanket -keep is the simplest correct rule.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# FirebaseAuth, FirebaseFirestore, FirebaseDatabase, FirebaseStorage, Crashlytics
# each ship their own consumer rules but the umbrella -keep above makes any
# gap in those consumer rules harmless.

# Firebase App Check: the new key attestation provider reads via reflection.
-keep class com.google.firebase.appcheck.** { *; }

# Firebase + Kotlin coroutines generic signatures. R8 8.x in full mode has been known to flag
# "applied generic arguments have different count than the expected formals" on Firebase
# coroutine internals when the kotlin-stdlib on the classpath is older than the one Firebase was
# compiled against. Keeping the coroutine runtime types preserves their generic metadata and
# avoids the validation error.
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleTypeAnnotations,AnnotationDefault

# Strip verbose / debug log calls in release builds. Log.d and Log.v calls in the app are
# diagnostic only and have no side effects; removing them shrinks the release APK and stops
# any future debug log from accidentally leaking data to logcat. Log.i / Log.w / Log.e are
# KEPT because the app's production error / info logs are real diagnostics (e.g. the wallet
# socket lifecycle, crash report manager messages). Anything sensitive must already go through
# LogSanitizer in the source.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}

# WorkManagerInitializer is loaded by its exact name from the merged manifest.
# A -keep on its own was not enough: CodeAssist's R8 still removed the class because no Java
# called it. SketchApplication now calls WorkManagerInitializer.create(), which is a real use,
# and the manifest entry for that class is removed. These rules stay so the name cannot be
# shortened if a build still merges the old startup entry.
-keep public class androidx.work.WorkManagerInitializer {
    public <init>();
    public *;
}
-keep public class androidx.work.impl.WorkManagerInitializer {
    public <init>();
}
-keep public class * implements androidx.startup.Initializer {
    public <init>();
}
-keep public class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }
-keep class androidx.startup.** { *; }
