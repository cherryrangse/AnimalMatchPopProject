# ==============================================================================
# R8 / ProGuard Rules for Animal Match Pop
# ==============================================================================

# 1. Google Mobile Ads (AdMob)
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# 2. Google User Messaging Platform (UMP / GDPR Consent)
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**

# 3. Google Play Billing (7.x)
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.**

# 4. AndroidX DataStore Preferences & Protobuf
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-keep class androidx.datastore.** { *; }

# 5. Kotlin Coroutines & Compose Runtime
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
