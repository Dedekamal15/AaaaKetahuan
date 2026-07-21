# ─── Metadata & Signatures ──────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Exceptions

# ─── MPAndroidChart ─────────────────────────────────────────────────
-keep class com.github.mikephil.charting.** { *; }

# ─── Google API Client & Sheets ─────────────────────────────────────
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.sheets.** { *; }
-keep class com.google.auth.** { *; }
-keep class com.google.http.client.** { *; }
-dontwarn com.google.api.client.**

# ─── Google Play Services / Google Sign-In ──────────────────────────
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.api.** { *; }
-keep class com.google.android.gms.auth.api.signin.** { *; }

# ─── Gson (used in TransaksiRepository for TypeToken & JSON) ────────
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ─── Google Sign-In credential & transport classes ──────────────────
-keep class com.google.api.client.googleapis.extensions.android.gms.auth.** { *; }
-keep class com.google.api.client.googleapis.javanet.** { *; }
-keep class com.google.api.client.json.gson.** { *; }

# Apache HTTP: optional references, safe to ignore
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-dontwarn com.google.api.client.extensions.android.**
