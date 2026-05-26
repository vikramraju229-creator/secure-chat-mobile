# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# SQLCipher
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**

# Hilt
-keep class dagger.hilt.android.internal.** { *; }

# Compose
-keep class androidx.compose.runtime.** { *; }

# General
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
