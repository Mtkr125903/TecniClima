# AirService PRO - reglas de ProGuard/R8

# Room genera implementaciones por reflexion en tiempo de ejecucion.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Modelos de datos serializados por Gson (respaldos JSON).
-keep class com.airservice.pro.data.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# Material Components
-dontwarn com.google.android.material.**
