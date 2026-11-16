# R8 / ProGuard rules for the release build.
# These are applied on top of the default `proguard-android-optimize.txt` rules.

# ---------------------------------------------------------------------------
# Diagnostics
# ---------------------------------------------------------------------------
# Keep line numbers so release stack traces can be deobfuscated with the
# mapping file, but do not leak the original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# General
# ---------------------------------------------------------------------------
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# Kotlin metadata (used by reflection-based libraries).
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Lazy {
    <fields>;
}

# Give useful names to lambda / companion classes in crash reports.
-keepnames class **$$serializer
-keepnames class *$Companion

# ---------------------------------------------------------------------------
# Navigation Compose & kotlinx.serialization
# ---------------------------------------------------------------------------
# Keep classes annotated with @Serializable and @Keep
-keep @kotlinx.serialization.Serializable class * { *; }
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
    @androidx.annotation.Keep *;
}
-keepclassmembers class **$$serializer {
    *** INSTANCE;
}
-keep,allowobfuscation,allowoptimization class * {
    @kotlinx.serialization.SerialName *;
}

# Keep Navigation Routes and arguments
-keep class ir.heydar.monetra.core.navigation.** { *; }
-keepclassmembers class ir.heydar.monetra.core.navigation.** { *; }

# ---------------------------------------------------------------------------
# Domain Models & Enums
# ---------------------------------------------------------------------------
# Keep domain models and enums so serialization, Navigation args, and Room converters work by name
-keep class ir.heydar.monetra.domain.model.** { *; }
-keepclassmembers enum ir.heydar.monetra.domain.model.** { *; }

# ---------------------------------------------------------------------------
# Room Database & Entities & DAOs
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep class ir.heydar.monetra.data.local.dao.** { *; }
-keep class ir.heydar.monetra.data.local.entity.** { *; }
-keepclassmembers class ir.heydar.monetra.data.local.entity.** { *; }
-keep class ir.heydar.monetra.data.local.converter.** { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# WorkManager
# ---------------------------------------------------------------------------
# Workers are instantiated by name, so keep the names stable.
-keep class * extends androidx.work.ListenableWorker { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(...);
}

# ---------------------------------------------------------------------------
# Third-party Libraries
# ---------------------------------------------------------------------------
# PersianDate
-keep class com.github.samanzamani.persiandate.** { *; }

# ---------------------------------------------------------------------------
# DataStore / kotlinx.coroutines
# ---------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
