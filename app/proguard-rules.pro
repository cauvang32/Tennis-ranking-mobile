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

# === Moshi ===
# Keep Moshi @JsonClass generated adapters
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
# Keep all @JsonClass annotated classes and their generated adapters
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class **JsonAdapter { *; }
# Keep Kotlin metadata for Moshi reflection
-keepclassmembers class kotlin.Metadata { *; }

# === Retrofit ===
# Keep Retrofit service interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Keep generic type signatures for Retrofit response parsing
-keepattributes Signature
-keepattributes Exceptions

# === OkHttp ===
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# === App API models ===
# Keep all data classes used for API serialization
-keep class com.example.api.** { *; }
