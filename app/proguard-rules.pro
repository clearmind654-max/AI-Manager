# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aimanager.**$$serializer { *; }
-keepclassmembers class com.aimanager.** {
    *** Companion;
}
-keepclasseswithmembers class com.aimanager.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room entities
-keep class com.aimanager.data.entity.** { *; }
-keep class com.aimanager.core.model.** { *; }

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# API response models
-keep class com.aimanager.core.network.provider.** { *; }
