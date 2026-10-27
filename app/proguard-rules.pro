# Project-specific R8 rules (the default proguard-android-optimize.txt rules also apply).

# OkHttp's Bouncy Castle TLS integration is optional on Android.
# The classes below are only referenced when that optional provider is present.
-dontwarn org.bouncycastle.jsse.**

# OkHttp also contains optional Conscrypt and OpenJSSE platform integrations.
# These providers are not bundled by the app and are not required on Android.
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Keep line numbers in crash stack traces. They can be de-obfuscated with the R8 mapping file
# written to app/build/outputs/mapping/release for the build that produced the APK.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
