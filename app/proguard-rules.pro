# Add project specific ProGuard rules here.
# Keep data classes
-keep class com.teeshirtminute.whatsappauto.data.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
