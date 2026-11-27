# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Firebase Auth & Firestore Keep Rules
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.firestore.** { *; }
-keep class io.grpc.** { *; }
-keepattributes *Annotation*,Signature

# OkHttp & Moshi Keep Rules
-keepattributes *JavascriptInterface*
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Model classes
-keep class com.example.model.** { *; }
-keep class com.example.data.** { *; }
