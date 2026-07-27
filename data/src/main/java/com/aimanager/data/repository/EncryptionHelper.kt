package com.aimanager.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        val spec = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        MasterKey.Builder(context).setKeyGenParameterSpec(spec).build()
    }

    private val prefs by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                "ai_manager_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // If encrypted prefs are corrupted, delete and recreate
            context.deleteSharedPreferences("ai_manager_secure_prefs")
            EncryptedSharedPreferences.create(
                context,
                "ai_manager_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun encrypt(value: String): String {
        val key = "enc_${value.hashCode()}"
        prefs.edit().putString(key, value).apply()
        return key
    }

    fun decrypt(key: String): String {
        return prefs.getString(key, "") ?: ""
    }

    fun deleteEncrypted(key: String) {
        prefs.edit().remove(key).apply()
    }
}
