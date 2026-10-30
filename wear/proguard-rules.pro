# Project-specific R8 rules (the default proguard-android-optimize.txt rules also apply).

# Keep line numbers in crash stack traces. They can be de-obfuscated with the R8 mapping file
# written to wear/build/outputs/mapping/release for the build that produced the APK.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
