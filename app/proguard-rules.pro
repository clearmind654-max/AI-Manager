# AI Manager ProGuard rules

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * implements dagger.hilt.android.internal.lifecycle.GeneratedComponent { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep class hilt_aggregated_deps.** { *; }

# Keep Kotlin metadata
-keepclassmembers class ** {
    *** Companion;
}

# Keep all serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    static final java.io.ObjectStreamField[] serialPersistentFields;
    private static final java.io.ObjectStreamField[] $serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Room database
-keep class com.aimanager.data.database.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers @androidx.room.Dao class * {
    <methods>;
}

# Keep ViewModel and LiveData
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class * extends androidx.lifecycle.AndroidViewModel { <init>(...); }

# Keep Compose classes
-keep class androidx.compose.** { *; }
-keep @androidx.compose.runtime.Composable class * { *; }

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
