# Add project specific ProGuard rules here.
# For more details, see https://developer.android.com/build/shrink-code

# --- Kotlin metadata & coroutines ---------------------------------------------------------
-keepclassmembers class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**

# --- kotlinx.serialization ----------------------------------------------------------------
# Keep generated serializers and the companion .serializer() accessors for @Serializable types.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers,allowshrinking class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep the app's serializable models and their nested serializers.
-keep,includedescriptorclasses class com.finix.paxdevicereaderapplication.**$$serializer { *; }
-keepclassmembers class com.finix.paxdevicereaderapplication.data.** {
    *** Companion;
}

# --- Hilt / Dagger ------------------------------------------------------------------------
# Hilt ships its own consumer rules, so no explicit keeps are usually required. These guard
# against warnings from generated code.
-dontwarn dagger.hilt.**
-dontwarn javax.annotation.**

# --- Finix Pax Device Reader SDK ----------------------------------------------------------
# Keep the SDK's public API surface; the vendor driver is loaded reflectively at runtime.
-keep class com.finix.** { *; }
-dontwarn com.finix.**
