package hi.baka3k.security.pki

import hi.baka3k.security.ec.BouncyCastle
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSAlgorithm
import org.bouncycastle.cms.CMSEnvelopedData
import org.bouncycastle.cms.CMSEnvelopedDataGenerator
import org.bouncycastle.cms.CMSException
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.CMSSignedDataParser
import org.bouncycastle.cms.CMSTypedData
import org.bouncycastle.cms.KeyTransRecipientInformation
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.operator.OperatorCreationException
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.bouncycastle.util.Selector
import org.bouncycastle.util.Store
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.logging.Logger


class PKI {
    private val LOG = Logger.getLogger(BouncyCastle::class.java.name)

    init {
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 0)
    }

    fun loadKeyPairFromPem(pemImputStream: InputStream): Pair<ECPrivateKeyParameters, ECPublicKeyParameters> {
        val pemParser = PEMParser(InputStreamReader(pemImputStream))
        val pemKeyPair = pemParser.readObject() as PEMKeyPair
        // Convert to Java (JCA) format
        val converter = JcaPEMKeyConverter()
        val keyPair = converter.getKeyPair(pemKeyPair)
        pemParser.close()
        val privateKey: ECPrivateKey = keyPair.private as ECPrivateKey
        val publicKey: ECPublicKey = keyPair.public as ECPublicKey
        val privateKey1 = ECUtil.generatePrivateKeyParameter(privateKey) as ECPrivateKeyParameters
        val publicKey1 = ECUtil.generatePublicKeyParameter(publicKey) as ECPublicKeyParameters

        return Pair(privateKey1, publicKey1)
    }

    fun verifySignatureCMS(signed: ByteArray, cert: Certificate): Boolean {
        try {
            val parser = CMSSignedDataParser(
                JcaDigestCalculatorProviderBuilder().setProvider("BC").build(), signed
            )
            parser.getSignedContent().drain()// must to call before check sign
            val signers = parser.getSignerInfos()
            val signerCollection: Collection<*> = signers.signers
            val it = signerCollection.iterator()
            var verified = false
            val certHolder = X509CertificateHolder(cert.encoded)
            val verifier = JcaSimpleSignerInfoVerifierBuilder().setProvider("BC")
                .build(certHolder)
            while (it.hasNext()) {
                val signerInformation = it.next() as SignerInformation
                if (!verified) {
                    verified = signerInformation.verify(verifier)
                }
                if (verified) {
                    break
                }
            }
            return verified
        } catch (e: CMSException) {
            LOG.warning("$TAG #verifySignatureCMS() err:${e.message}")
            return false
        } catch (e: IOException) {
            LOG.warning("$TAG #verifySignatureCMS() err:${e.message}")
            return false
        } catch (e: OperatorCreationException) {
            LOG.warning("$TAG #verifySignatureCMS() err:${e.message}")
            return false
        } catch (e: CertificateException) {
            LOG.warning("$TAG #verifySignatureCMS() err:${e.message}")
            return false
        }
    }

    fun loadX509Certificate(inputStreamX509Certificate: InputStream): X509Certificate {
        val certFactory = CertificateFactory.getInstance("X.509", "BC")
        val certificate = certFactory.generateCertificate(inputStreamX509Certificate) as X509Certificate
        LOG.warning("$TAG #loadX509Certificate() $certificate")
        LOG.warning("$TAG loadX509Certificate() sigAlgName ${certificate.sigAlgName}")
        LOG.warning("$TAG loadX509Certificate() ${certificate.sigAlgOID}")
        LOG.warning("$TAG loadX509Certificate() ${certificate.sigAlgOID}")
        LOG.warning("$TAG loadX509Certificate() ${certificate.subjectDN}")
        LOG.warning("$TAG loadX509Certificate() ${certificate.issuerDN}")
        return certificate
    }

    fun loadPrivateKey(inputStreamPKCS12: InputStream, keystorePassword: CharArray, keyPassword: CharArray, alias: String): PrivateKey {
        val keystore = KeyStore.getInstance("PKCS12")
        keystore.load(inputStreamPKCS12, keystorePassword)
        val key = keystore.getKey(alias, keyPassword) as PrivateKey
        LOG.warning("$TAG #loadPrivateKey() PrivateKey: ${key}")
        return key
    }

    fun verify(signature: ByteArray): Boolean {
        try {
//            val bIn = ByteArrayInputStream(signature)
//            val aIn = ASN1InputStream(bIn)
//            val s = CMSSignedData(ContentInfo.getInstance(aIn.readObject()))
//            aIn.close()
//            bIn.close()
//
//            val certs = s.certificates
//            val signers = s.getSignerInfos()
//            val c = signers.signers
//            val signer = c.iterator().next()
//            val selector = signer.sid as Selector<X509CertificateHolder>
//            val certCollection = certs.getMatches(selector)
//            val certIt = certCollection.iterator()
//            val certHolder = certIt.next()
//            return signer.verify(JcaSimpleSignerInfoVerifierBuilder().build(certHolder))

            val digestCalculatorProvider = BcDigestCalculatorProvider()
            val cmssSignParser = CMSSignedDataParser(digestCalculatorProvider, signature)
            cmssSignParser.getSignedContent().drain()
            val certs = cmssSignParser.certificates
            val signers = cmssSignParser.getSignerInfos()
            val c = signers.signers
            val signer = c.iterator().next()
            val selector = signer.sid as Selector<X509CertificateHolder>
            val certCollection = certs.getMatches(selector)
            val certIt = certCollection.iterator()
            val certHolder = certIt.next() as X509CertificateHolder
            return signer.verify(JcaSimpleSignerInfoVerifierBuilder().build(certHolder))
        } catch (e: CMSException) {
            LOG.warning("$TAG #verify() err:${e.message}")
            return false
        } catch (e: IOException) {
            LOG.warning("$TAG #verify() err:${e.message}")
            return false
        } catch (e: OperatorCreationException) {
            LOG.warning("$TAG #verify() err:${e.message}")
            return false
        } catch (e: CertificateException) {
            LOG.warning("$TAG #verify() err:${e.message}")
            return false
        }

    }

    /**
     * signatureAlgorithm : SHA256withECDSA, SHA256withRSA
     * */
    fun sign(inputData: ByteArray, signingCertificate: X509Certificate, signingKey: PrivateKey, signatureAlgorithm: String = "SHA256withECDSA"): ByteArray {
        try {
            val cmsData: CMSTypedData = CMSProcessableByteArray(inputData)
            val cmsGenerator = CMSSignedDataGenerator()
            val csBuilder = JcaContentSignerBuilder(signatureAlgorithm)
            val signer = csBuilder.setProvider("BC").build(signingKey)
            val jaSignBuilder = JcaSignerInfoGeneratorBuilder(
                JcaDigestCalculatorProviderBuilder().build()
            ).build(signer, signingCertificate)
            cmsGenerator.addSignerInfoGenerator(jaSignBuilder)
            val certList: MutableList<X509Certificate> = ArrayList()
            certList.add(signingCertificate)
            val certs: Store<*> = JcaCertStore(certList)

            cmsGenerator.addCertificates(certs)
            val cms = cmsGenerator.generate(cmsData, true)
            return cms.encoded
        } catch (e: CertificateEncodingException) {
            LOG.warning("$TAG #signData() err:${e.message}")
            LOG.warning("$TAG #signData() bypass for test - return original value")
            return inputData
        } catch (e: OperatorCreationException) {
            LOG.warning("$TAG #signData() err:${e.message}")
            LOG.warning("$TAG #signData() bypass for test - return original value")
            return inputData
        } catch (e: CMSException) {
            LOG.warning("$TAG #signData() err:${e.message}")
            LOG.warning("$TAG #signData() bypass for test - return original value")
            return inputData
        } catch (e: IOException) {
            LOG.warning("$TAG #signData() err:${e.message}")
            LOG.warning("$TAG #signData() bypass for test - return original value")

            return inputData
        }
    }

    fun encryptData(originInput: ByteArray, encryptionCertificate: X509Certificate): ByteArray {
        try {
            val cmsEnvelopedDataGenerator = CMSEnvelopedDataGenerator()
            val jceKey = JceKeyTransRecipientInfoGenerator(encryptionCertificate)
            cmsEnvelopedDataGenerator.addRecipientInfoGenerator(jceKey)
            val msg = CMSProcessableByteArray(originInput)
            val encryptor = JceCMSContentEncryptorBuilder(CMSAlgorithm.AES128_CBC).setProvider("BC").build()
            val cmsEnvelopedData = cmsEnvelopedDataGenerator.generate(msg, encryptor)
            return cmsEnvelopedData.encoded
        } catch (e: CertificateEncodingException) {
            LOG.warning("$TAG #encryptData() err:${e.message}")
            LOG.warning("$TAG #encryptData() bypass for test - return original value")
            return originInput
        } catch (e: CMSException) {
            LOG.warning("$TAG #encryptData() err:${e.message}")
            LOG.warning("$TAG #encryptData() bypass for test - return original value")
            return originInput
        } catch (e: IOException) {
            LOG.warning("$TAG #encryptData() err:${e.message}")
            LOG.warning("$TAG #encryptData() bypass for test - return original value")
            return originInput
        }
    }

    fun decryptData(encryptedData: ByteArray, decryptionKey: PrivateKey): ByteArray {
        return try {
            val envelopedData = CMSEnvelopedData(encryptedData)
            val recipients = envelopedData.recipientInfos.recipients
            val recipientInfo = recipients.iterator().next() as KeyTransRecipientInformation
            val recipient = JceKeyTransEnvelopedRecipient(decryptionKey)
            recipientInfo.getContent(recipient)
        } catch (e: CMSException) {
            LOG.warning("$TAG #decryptData() err:${e.message}")
            LOG.warning("$TAG #decryptData() bypass for test - return original value")
            encryptedData
        }
    }

    fun extractPublicKey(signatureData: ByteArray): PublicKey {
        val digestCalculatorProvider = BcDigestCalculatorProvider()
        val cmssSignParser = CMSSignedDataParser(digestCalculatorProvider, signatureData)
        val signed = cmssSignParser.signedContent
        val encodedPublicKey = signed.contentStream
        val encodedPublicKeyAsByte = encodedPublicKey.readBytes()
        val converter = JcaPEMKeyConverter()
        val publicKeyInfo = SubjectPublicKeyInfo.getInstance(encodedPublicKeyAsByte)
        return converter.getPublicKey(publicKeyInfo)
    }

    companion object {
        const val TAG = "PKI"
    }
}