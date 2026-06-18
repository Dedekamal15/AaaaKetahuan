# Add project specific ProGuard rules here.

# Gson: keep model fields from obfuscation
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.aaaaketahuan.data.model.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
