package store.oatec.playertv.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject
import store.oatec.playertv.model.PlayerSource
import store.oatec.playertv.model.SourceType

class SecureSourceStore(context: Context) {
    private val preferences = context.getSharedPreferences("player_sources", Context.MODE_PRIVATE)

    fun load(): List<PlayerSource> {
        val encrypted = preferences.getString(SOURCES_KEY, null) ?: return emptyList()
        val json = decrypt(encrypted)
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val type = runCatching { SourceType.valueOf(item.optString("type")) }.getOrNull() ?: continue
                add(
                    PlayerSource(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        type = type,
                        serverUrl = item.optString("serverUrl"),
                        username = item.optString("username"),
                        password = item.optString("password"),
                    ),
                )
            }
        }
    }

    fun save(sources: List<PlayerSource>) {
        val array = JSONArray()
        sources.forEach { source ->
            array.put(
                JSONObject()
                    .put("id", source.id)
                    .put("name", source.name)
                    .put("type", source.type.name)
                    .put("serverUrl", source.serverUrl)
                    .put("username", source.username)
                    .put("password", source.password),
            )
        }
        preferences.edit().putString(SOURCES_KEY, encrypt(array.toString())).apply()
    }

    fun loadPremiumCode(): String? = preferences.getString(PREMIUM_CODE_KEY, null)?.let(::decrypt)

    fun savePremiumCode(code: String) {
        if (code.isBlank()) {
            preferences.edit().remove(PREMIUM_CODE_KEY).apply()
        } else {
            preferences.edit().putString(PREMIUM_CODE_KEY, encrypt(code.trim())).apply()
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        val iv = payload.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = payload.copyOfRange(GCM_IV_LENGTH, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val SOURCES_KEY = "encrypted_sources"
        const val PREMIUM_CODE_KEY = "encrypted_premium_code"
        const val KEY_ALIAS = "oatec.player.sources"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_LENGTH = 128
    }
}