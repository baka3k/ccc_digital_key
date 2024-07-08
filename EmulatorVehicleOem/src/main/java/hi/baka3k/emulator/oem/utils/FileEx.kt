package hi.baka3k.emulator.oem.utils

import android.util.Base64
import hi.baka3k.security.ec.toECPrivateKeyParameters
import hi.baka3k.security.ec.toECPublicKeyParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.lang.reflect.InvocationTargetException
import java.nio.charset.Charset
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException

fun File.writePublicKey(encoder: ByteArray) {
    val base64PubKey = Base64.encodeToString(encoder, Base64.DEFAULT)
    val data = "-----BEGIN PUBLIC KEY-----\n" + base64PubKey.replace(
        "(.{64})".toRegex(),
        "$1\n"
    ) + "\n-----END PUBLIC KEY-----\n"
    writeData(data.toByteArray())
}

fun File.writeSecretKey(encoder: ByteArray) {
    val base64PubKey = Base64.encodeToString(encoder, Base64.DEFAULT)
    val data = "-----BEGIN PRIVATE KEY-----\n" +
            base64PubKey.replace("(.{64})".toRegex(), "$1\n") +
            "\n-----END PRIVATE KEY-----\n"
    writeData(data.toByteArray())
}

fun File.writeData(encoder: ByteArray) {
    val fos = FileOutputStream(this)
    fos.write(encoder)
    fos.close()
}

fun File.toSecretKey(): ECPrivateKeyParameters? {
    try {
        val key = this.readText(Charset.defaultCharset())
        val pemString: String =
            key.replace("-----BEGIN PRIVATE KEY-----", "").replace(System.lineSeparator(), "")
                .replace("-----END PRIVATE KEY-----", "")
        val encoded: ByteArray = Base64.decode(pemString.trim(), Base64.DEFAULT)
        return encoded.toECPrivateKeyParameters()
    } catch (e: InvalidKeySpecException) {
        return null
    } catch (e: NoSuchAlgorithmException) {
        return null
    } catch (e: NullPointerException) {
        return null
    } catch (e: InvocationTargetException) {
        return null
    } catch (e: FileNotFoundException) {
        return null
    }
}

fun File.toPublicKey(): ECPublicKeyParameters? {
    try {
        val key = this.readText(Charset.defaultCharset())
        val publicKeyPEM: String =
            key.replace("-----BEGIN PUBLIC KEY-----", "").replace(System.lineSeparator(), "")
                .replace("-----END PUBLIC KEY-----", "")
        val encoded: ByteArray = Base64.decode(publicKeyPEM.trim(), Base64.DEFAULT)
        return encoded.toECPublicKeyParameters()
    } catch (e: InvalidKeySpecException) {
        return null
    } catch (e: NoSuchAlgorithmException) {
        return null
    } catch (e: NullPointerException) {
        return null
    } catch (e: InvocationTargetException) {
        return null
    } catch (e: FileNotFoundException) {
        return null
    }
}