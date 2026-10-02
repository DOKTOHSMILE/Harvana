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
