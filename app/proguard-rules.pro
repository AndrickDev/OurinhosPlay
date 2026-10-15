# kotlinx.serialization: mantém os serializers gerados dos modelos do app.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep,includedescriptorclasses class com.ourinhosplay.app.**$$serializer { *; }
-keepclassmembers class com.ourinhosplay.app.** { *** Companion; }
-keepclasseswithmembers class com.ourinhosplay.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Ktor / Supabase / Credential Manager: avisos de classes opcionais.
-dontwarn io.ktor.**
-dontwarn org.slf4j.**
-dontwarn kotlinx.coroutines.debug.**
-dontwarn java.lang.management.**
