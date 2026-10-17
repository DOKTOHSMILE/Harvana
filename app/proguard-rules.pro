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

# Play services references this annotation; it is not in the app. A missing class here is a warning, not a crash.
-dontwarn com.google.android.apps.common.proguard.SideEffectFree

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
