# Consumer rules propagated to the app's R8 step when it minifies a release build.
# Library-local proguard-rules.pro is unused because :data has isMinifyEnabled = false.

# kotlinx.serialization — keep generated $$serializer companions for @Serializable types in :data.
-keepclassmembers @kotlinx.serialization.Serializable class com.emm.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.emm.data.**$$serializer { *; }

# SQLDelight generated database surface (HelloDb + adapters live under com.emm.data).
-keep class com.emm.data.HelloDb { *; }
-keep class com.emm.data.HelloDbImpl* { *; }
