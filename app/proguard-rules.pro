# Room, Compose, Navigation and kotlinx.serialization ship their own consumer rules.
# Keep the @Serializable navigation routes (their serializers are looked up reflectively).
-keepclassmembers @kotlinx.serialization.Serializable class com.fandom.auchatstudio.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.fandom.auchatstudio.**$$serializer { *; }
