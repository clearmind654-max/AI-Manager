# AI Manager ProGuard rules

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * implements dagger.hilt.android.internal.lifecycle.GeneratedComponent { *; }

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

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
