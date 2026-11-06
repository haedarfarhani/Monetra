package ir.heydar.monetra.core.text

import java.security.MessageDigest

/** Stable fingerprint of a normalized bank message: identical imports must never duplicate rows. */
object MessageFingerprint {

    fun of(normalizedMessage: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(normalizedMessage.trim().toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
