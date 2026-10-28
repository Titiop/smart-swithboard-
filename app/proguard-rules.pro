# Project specific ProGuard rules

# Firebase Realtime Database
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.database.IgnoreExtraProperties *;
    @com.google.firebase.database.PropertyName *;
}
-keepclassmembers class * {
    public <init>();
    public void set*(...);
    public boolean is*();
    public * get*();
}
-keep class com.google.firebase.database.** { *; }
-keep class com.google.firebase.auth.** { *; }

# AndroidX Biometric
-keep class androidx.biometric.** { *; }

