# SecureChat ProGuard / R8 Rules

# ---- Domain Models (serialization) ----
-keep class com.securechat.app.domain.model.** { *; }

# ---- Network DTOs ----
-keep class com.securechat.app.model.** { *; }

# ---- Room Entities ----
-keep class com.securechat.app.data.local.** { *; }

# ---- Crypto ----
-keep class com.securechat.app.crypto.** { *; }
-keep class com.securechat.app.core.security.** { *; }

# ---- Firebase ----
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ---- Gson ----
-keepattributes Signature, *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ---- OkHttp / Okio ----
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ---- Retrofit ----
-keepattributes Exceptions, InnerClasses
-keep class retrofit2.** { *; }

# ---- Hilt / Dagger ----
-dontwarn dagger.**
-dontwarn javax.inject.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ---- Compose ----
-dontwarn androidx.compose.**

# ---- Kotlin Coroutines ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ---- Keep Application entry point ----
-keep class com.securechat.app.SecureChatApplication { *; }
-keep class com.securechat.app.MainActivity { *; }

# ---- Remove logging in release ----
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
