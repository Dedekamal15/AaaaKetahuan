# Add project specific ProGuard rules here.

# Gson: keep model fields from obfuscation
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.aaaaketahuan.data.model.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Google API Client
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.sheets.** { *; }
-keep class com.google.auth.** { *; }
-keep class com.google.http.client.** { *; }
-dontwarn com.google.api.client.**
