# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# OkHttp's Bouncy Castle TLS integration is optional on Android.
# The classes below are only referenced when that optional provider is present.
-dontwarn org.bouncycastle.jsse.**

# OkHttp also contains optional Conscrypt and OpenJSSE platform integrations.
# These providers are not bundled by the app and are not required on Android.
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
