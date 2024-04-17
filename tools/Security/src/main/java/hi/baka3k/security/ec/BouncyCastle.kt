package hi.baka3k.security.ec

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.DataLengthException
import org.bouncycastle.crypto.EphemeralKeyPair
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.generators.EphemeralKeyPairGenerator
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.generators.KDF2BytesGenerator
import org.bouncycastle.crypto.generators.SCrypt
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECKeyGenerationParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KDFParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.math.BigInteger
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import java.util.logging.Logger
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec

class BouncyCastle {
    private val LOG = Logger.getLogger(BouncyCastle::class.java.name)
    private val ecDSASigner = ECDSASigner()
    private val secureRandom = SecureRandom()

    // in test phase, we choose the fix IV bytearray
    private val IV = byteArrayOf(11, 22, 33, 44, 55, 66, 88, 88, 99, 11, 11, 11, 11, 11, 11, 11)
    private val ecKeyPairGenerator = ECKeyPairGenerator()
    private val ecKeyGenerationParameters =
        ECKeyGenerationParameters(ecDomainParameters, secureRandom)

    init {
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 0)
        ecKeyPairGenerator.init(ecKeyGenerationParameters)
    }

    /**
     * 18.1.2 Execution
     * z0=left 40 bytes of Scrypt(pwd, s, Nscrypt, r, p, dkLen)
     * z1=right 40 bytes of Scrypt(pwd, s, Nscrypt, r, p, dkLen)
     * w0 = (z0 mod (n-1)) + 1 with n being the order n of base point G as defined for NIST P-256
     * w1 = (z1 mod (n-1)) + 1 with n being the order n of base point G as defined for NIST P-256
     * w0 and w1
     * oderBasePointG = curveParam.n
     * L = w1 × G
     * */
    fun caculatew0w1(scrypt: ByteArray): Pair<BigInteger, BigInteger> {
        val scryptSize = scrypt.size

        val z0asByte = scrypt.copyOfRange(0, 40)
        val z1asByte = scrypt.copyOfRange(scryptSize - 40, scryptSize)

        val z0 = z0asByte.toBigInteger()
        val z1 = z1asByte.toBigInteger()

        val n = curveParam.n

        val n_1 = n - BigInteger.ONE
        val w0 = (z0 % n_1) + BigInteger.ONE
        val w1 = (z1 % n_1) + BigInteger.ONE
        caculateL(w1)
//        Log.d("test", "L:($L)")
//        Log.d("test", "LX:(${L.xCoord})")
//        Log.d("test", "LY:(${L.yCoord})")
        return Pair(w0, w1)
    }

    fun caculateL(w1: BigInteger): ECPoint {
        val g = curveParam.g
        val L = g.multiply(w1)
        //Log.d("test", "L:($L)")
        //Log.d("test", "LX:(${L.xCoord})")
        //Log.d("test", "LY:(${L.yCoord})")
        return L
    }

    fun encrypt(
        input: ByteArray, publicKey: ECPublicKeyParameters, privateKey: ECPrivateKeyParameters
    ): ByteArray {
        try {
            val sharedSecret = createSharedSecret(publicKey, privateKey)
            val kdf = KDF2BytesGenerator(SHA256Digest())
            kdf.init(KDFParameters(sharedSecret, IV))
            val derivedKey = ByteArray(32) // AES-256
            kdf.generateBytes(derivedKey, 0, derivedKey.size)
            val cipher = cipher()
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derivedKey, "AES"))
            return cipher.doFinal(input)
        } catch (e: IllegalArgumentException) {
            LOG.warning("$TAG #encrypt() err ${e.message}")
            return input
        } catch (e: DataLengthException) {
            LOG.warning("$TAG #encrypt() err ${e.message}")
            return input
        } catch (e: NoSuchPaddingException) {
            LOG.warning("$TAG #encrypt() err ${e.message}")
            return input
        } catch (e: NoSuchProviderException) {
            LOG.warning("$TAG #encrypt() err ${e.message}")
            return input
        } catch (e: NoSuchAlgorithmException) {
            LOG.warning("$TAG #encrypt() err ${e.message}")
            return input
        } catch (e: UnsupportedOperationException) {
            LOG.warning("$TAG #encrypt() err ${e.message}")
            return input
        } catch (e: InvalidKeyException) {
            LOG.warning("$TAG #encrypt() err ${e.message}")
            return input
        }
    }

    fun decrypt(
        cippher: ByteArray, publicKey: ECPublicKeyParameters, privateKey: ECPrivateKeyParameters
    ): ByteArray {
        try {
            val sharedSecret = createSharedSecret(publicKey, privateKey)
            val kdf = KDF2BytesGenerator(SHA256Digest())
            kdf.init(KDFParameters(sharedSecret, IV))
            val derivedKey = ByteArray(32) // AES-256
            kdf.generateBytes(derivedKey, 0, derivedKey.size)
            val cipher = cipher()
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derivedKey, "AES"))
            return cipher.doFinal(cippher)
        } catch (e: IllegalArgumentException) {
            LOG.warning("$TAG #decrypt() err ${e.message}")
            return cippher
        } catch (e: DataLengthException) {
            LOG.warning("$TAG #decrypt() err ${e.message}")
            return cippher
        } catch (e: NoSuchPaddingException) {
            LOG.warning("$TAG #decrypt() err ${e.message}")
            return cippher
        } catch (e: NoSuchProviderException) {
            LOG.warning("$TAG #decrypt() err ${e.message}")
            return cippher
        } catch (e: NoSuchAlgorithmException) {
            LOG.warning("$TAG #decrypt() err ${e.message}")
            return cippher
        } catch (e: UnsupportedOperationException) {
            LOG.warning("$TAG #decrypt() err ${e.message}")
            return cippher
        } catch (e: InvalidKeyException) {
            LOG.warning("$TAG #decrypt() err ${e.message}")
            return cippher
        }
    }

    private fun cipher(): Cipher {
        return Cipher.getInstance(TRANSFORMATION, "BC")
    }

    fun createSharedSecret(
        publicKey: ECPublicKeyParameters, privateKey: ECPrivateKeyParameters
    ): ByteArray {
//        return publicKey.q.multiply(privateKey.d).getEncoded(false)
        val basicAgreement = ECDHBasicAgreement()
        basicAgreement.init(privateKey)
        val keyAgreement = basicAgreement.calculateAgreement(publicKey)
        return keyAgreement.toByteArray()
    }

    fun genEphemeralKeyPair(): EphemeralKeyPair {
        val generator = EphemeralKeyPairGenerator(
            ecKeyPairGenerator
        ) { keyParameter -> (keyParameter as ECPublicKeyParameters).q.getEncoded(false) }
        return generator.generate()
    }

    fun genECKeyPair(): AsymmetricCipherKeyPair {
        return ecKeyPairGenerator.generateKeyPair()
    }

    /**
     * 18.1.2 Execution
     * Salt: 16 bytes, randomly generated for each new verifier as per [12]
     * Cost parameter Nscrypt: 4096 or higher
     * Block size r: 8
     * Parallelization parameter p: 1
     * Output length dkLen: 80
     *
     * */
    fun genSCrypt(
        pass: ByteArray,
        s: ByteArray = IV,
        n: Int = 4096,
        r: Int = 8,
        p: Int = 1,
        keySize: Int = 80
    ): ByteArray {
        return SCrypt.generate(pass, s, n, r, p, keySize)
    }

    fun sign(input: ByteArray, privateKey: ECPrivateKeyParameters): ByteArray {
        ecDSASigner.init(true, privateKey)
        val signature = ecDSASigner.generateSignature(input)
        val r = signature[0].toByteArray()
        val s = signature[1].toByteArray()
        val signedData = ByteArray(r.size + s.size)
        System.arraycopy(r, 0, signedData, 0, r.size)
        System.arraycopy(s, 0, signedData, r.size, s.size)
        return signedData
    }

    fun verify(
        originalData: ByteArray,
        signature: ByteArray,
        publicKey: ECPublicKeyParameters
    ): Boolean {
        ecDSASigner.init(false, publicKey)
        var pair = extractSignData(signature, true)
        var verified = ecDSASigner.verifySignature(originalData, pair.first, pair.second)
        if (!verified) {
            pair = extractSignData(originalData, false)
            verified = ecDSASigner.verifySignature(originalData, pair.first, pair.second)
        }
        return verified
    }

    fun hkdfSha256(key: ByteArray, info: ByteArray, outputLength: Int): ByteArray {
        val ikm = key
        val output = ByteArray(outputLength)
        val params = HKDFParameters(ikm, IV, info)
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(params)
        hkdf.generateBytes(output, 0, outputLength)
        return output
    }

    /**
     * cod.Kenc ⟵ subset of derived_keys at offset 0 with length 16
     * */
    fun genKencKey(pk: ByteArray): ByteArray {
        val size = pk.size
        return if (size >= 16) {
            pk.copyOfRange(0, 16)
        } else {
            val newByteArray = ByteArray(size)
            pk.copyInto(newByteArray)
            newByteArray
        }
    }

    /**
     * cod.Kmac ⟵ subset of derived_keys at offset 16 with length 16
     * */
    fun genKmacKey(pk: ByteArray): ByteArray {
        val size = pk.size
        return if (size >= 32) {
            pk.copyOfRange(16, 32)
        } else if (size < 32 && size > 16) {
            pk.copyOfRange(size - 16, size)
        } else {
            val newByteArray = ByteArray(size)
            pk.copyInto(newByteArray)
            newByteArray
        }
    }

    /**
     * cod.Krmac ⟵ subset of derived_keys at offset 32 with length 16
     * */
    fun genKrmacKey(pk: ByteArray): ByteArray {
        val size = pk.size
        return if (size >= 48) {
            pk.copyOfRange(32, 48)
        } else if (size < 48 && size > 16) {
            pk.copyOfRange(size - 16, size)
        } else {
            val newByteArray = ByteArray(size)
            pk.copyInto(newByteArray)
            newByteArray
        }
    }

    /***********************************************************************************************
     * Support function
     **********************************************************************************************/

    fun extractSignData(signData: ByteArray, maxFirst: Boolean): Pair<BigInteger, BigInteger> {
        val size = signData.size
        val mod = size % 2
        val temp = size / 2
        return if (mod == 0) {
            val rB = signData.copyOfRange(0, temp)
            val sB = signData.copyOfRange(temp, signData.size)
            return Pair(BigInteger(rB), BigInteger(sB))
        } else {
            if (maxFirst) {
                val rB = signData.copyOfRange(0, temp + 1)
                val sB = signData.copyOfRange(temp + 1, signData.size)
                Pair(BigInteger(rB), BigInteger(sB))
            } else {
                val rB = signData.copyOfRange(0, temp)
                val sB = signData.copyOfRange(temp, signData.size)
                Pair(BigInteger(rB), BigInteger(sB))
            }
        }
    }

    companion object {
        const val TAG = "BouncyCastle"
        const val curveName = "P-256"
        const val TRANSFORMATION = "AES/ECB/PKCS7Padding"
        private val curveParam: X9ECParameters = CustomNamedCurves.getByName(curveName)
        val ecDomainParameters = ECDomainParameters(
            curveParam.curve, curveParam.g, curveParam.n, curveParam.h
        )
        private var instance: BouncyCastle? = null
        fun getInstance(): BouncyCastle {
            return instance ?: synchronized(this) {
                instance ?: BouncyCastle().also { instance = it }
            }
        }
    }
}

fun ECPublicKeyParameters.toByteArray(): ByteArray {
    val xCoordBytes = q.affineXCoord.encoded
    val yCoordBytes = q.affineYCoord.encoded
    // Gộp cả hai tọa độ lại thành mảng byte
    // Gộp cả hai tọa độ lại thành mảng byte
    val publicKeyBytes = ByteArray(xCoordBytes.size + yCoordBytes.size)
    System.arraycopy(xCoordBytes, 0, publicKeyBytes, 0, xCoordBytes.size)
    System.arraycopy(yCoordBytes, 0, publicKeyBytes, xCoordBytes.size, yCoordBytes.size)
    return publicKeyBytes
}

fun ECPrivateKeyParameters.toByteArray(): ByteArray {
    return this.d.toByteArray()
}

fun ByteArray.toECPublicKeyParameters(): ECPublicKeyParameters {
    return if (size == 65) {
        val newByte = copyOfRange(1, 65)
        newByte.toECPublicKeys()
    } else {
        this.toECPublicKeys()
    }
}

private fun ByteArray.toECPublicKeys(): ECPublicKeyParameters {
    val xCoordBytesReceived = this.copyOfRange(0, 32)
    val yCoordBytesReceived = this.copyOfRange(32, this.size)
    val curveName =
        "secp256r1"// BouncyCastle.curveName // example: elliptic curve prime256v1 (secp256r1)
    val curve = ECNamedCurveTable.getParameterSpec(curveName).curve

    val receivedPoint = curve.createPoint(
        BigInteger(1, xCoordBytesReceived), BigInteger(1, yCoordBytesReceived)
    )
    return ECPublicKeyParameters(receivedPoint, BouncyCastle.ecDomainParameters)
}

fun ByteArray.toECPrivateKeyParameters(): ECPrivateKeyParameters {
    val bigInteger = this.toBigInteger()
    return ECPrivateKeyParameters(bigInteger, BouncyCastle.ecDomainParameters)
}

fun ByteArray.toBigInteger(signum: Int = 1): BigInteger {
    return BigInteger(signum, this)
}

fun ByteArray.toPublicKey(): PublicKey {
    val converter = JcaPEMKeyConverter()
    val publicKeyInfo = SubjectPublicKeyInfo.getInstance(this)
    return converter.getPublicKey(publicKeyInfo)
}
