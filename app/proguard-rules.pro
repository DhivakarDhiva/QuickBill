# Proguard / R8 optimization rules for QuickBill POS

# Room Database
-keep class com.quickbill.pos.data.local.entity.** { *; }
-keep class com.quickbill.pos.data.local.dao.** { *; }
-keep class com.quickbill.pos.data.local.Converters { *; }
-keep class com.quickbill.pos.data.model.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Koin Dependency Injection
-keep class com.quickbill.pos.di.** { *; }
-dontwarn org.koin.**

# Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ML Kit Barcode Scanning & Vision
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }

# CameraX
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.view.** { *; }

# General optimizations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn sun.misc.Unsafe
