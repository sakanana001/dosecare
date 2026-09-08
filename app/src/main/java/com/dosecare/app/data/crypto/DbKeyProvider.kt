package com.dosecare.app.data.crypto

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SQLCipher 数据库密钥提供器
 *
 * 设计:
 * - 首次启动: SecureRandom 生成 32 字节随机 passphrase
 * - passphrase 加密后存 EncryptedSharedPreferences
 * - EncryptedSharedPreferences 内部用 Android Keystore (AES-256 GCM master key) 包装
 * - 启动时: 从 EncryptedSharedPreferences 读密文, 解密得到 passphrase (ByteArray)
 *
 * 安全模型:
 * - 设备未 root + Keystore hardware-backed → 安全
 * - 设备已 root → 等同明文 (Android 沙箱现实约束)
 *
 * 备份/恢复:
 * - 卸载重装 → 新随机密钥 → 老 DB 无法解密
 * - 将来可加 "exportDBKey" 给用户备份密钥
 */
@Singleton
class DbKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * 获取当前 SQLCipher passphrase.
     * 首次调用时生成 32 字节随机密钥, 后续调用返回已存的密钥.
     */
    fun getOrCreatePassphrase(): ByteArray {
        val existing = prefs.getString(KEY_PASSPHRASE_B64, null)
        if (existing != null) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }
        val newKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PASSPHRASE_B64, Base64.encodeToString(newKey, Base64.NO_WRAP))
            .apply()
        return newKey
    }

    /**
     * 测试 / 重置用: 清空 passphrase, 下次 getOrCreatePassphrase 会生成新密钥.
     * 注意: 新密钥将无法解密老 DB. 真实环境不要调用.
     */
    fun clearPassphrase() {
        prefs.edit().remove(KEY_PASSPHRASE_B64).apply()
    }

    companion object {
        private const val PREFS_NAME = "dosecare_db_crypto"
        private const val KEY_PASSPHRASE_B64 = "db_passphrase_v1"
    }
}
