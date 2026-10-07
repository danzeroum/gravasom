# R8 rules for the app module.
# Keep Media3 player classes accessed reflectively.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Tink uses reflection for key managers.
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Keep Hilt generated components entry points.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Room generated implementations.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Timber: strip verbose/debug in release.
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
}
