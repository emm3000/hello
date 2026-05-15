# Keep line numbers in stack traces so Crashlytics deobfuscation works.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization — official keep rules.
# https://github.com/Kotlin/kotlinx.serialization#android
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclasseswithmembers class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class <1>.<2> {
    <1>.<2>$Companion Companion;
}

# Project-level @Serializable types (nav routes under app, DTOs propagated via :data consumer-rules.pro).
-keepclassmembers @kotlinx.serialization.Serializable class com.emm.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.emm.**$$serializer { *; }

# Firebase — modern libs ship consumer rules, but keep public API defensively.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# SQLDelight runtime ships consumer rules; nothing extra needed here.
