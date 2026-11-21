-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.phimai.android.**$$serializer { *; }
-keepclassmembers class com.phimai.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.phimai.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }

-keep class androidx.media3.** { *; }
-keep class com.phimai.android.data.model.** { *; }
