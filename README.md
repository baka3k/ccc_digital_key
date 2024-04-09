# Getting started
The knowledge you need to know when starting a CCC-Digigal-Key Project, It will help you approach the project easier:
- Communication technology operating over a radio:
    -   NFC
    -   BLE
    -   UWB
- Basic Knowledge of Encryption Standards:
    -   [PKI](https://en.wikipedia.org/wiki/)
    -   [ECDSA](https://en.wikipedia.org/wiki/Elliptic_Curve_Digital_Signature_Algorithm)
    -   [NIST P-256](https://csrc.nist.gov/csrc/media/events/workshop-on-elliptic-curve-cryptography-standards/documents/papers/session6-adalier-mehmet.pdf)
- [Card Emulator](https://developer.android.com/develop/connectivity/nfc/hce)
- Application Protocol Data Unit(APDU): ApduCommand, ApduResponse
    -   [ISO 7816-4](https://en.wikipedia.org/wiki/Smart_card_application_protocol_data_unit)
    -   [TLV(Tag Length Value) data structure](https://en.wikipedia.org/wiki/Type%E2%80%93length%E2%80%93value)

# Steps OverView
**Step: 1 Prepare**: The prerequisite for executing this standard transaction is that the vehicle has obtained the KeyPair(Vehicle.Pk & Vehicle.Sk) from the **VehicleOEMServer**, and the **Mobile Phone**(EndPoint or Device) also has its own terminal's KeyPair (Endpoint.Pk & Endpoint.Sk). At the same time, Public keys have been exchanged when the Vehicles were paired with Mobile Phone 

><em>(Note that ONLY the **Public keys** are exchanged, the **private keys are NOT** - private keys are saved separately)</em>

Secondly, the App Applet(Digital key Applet) has been loaded in Secure Element. With the above prerequisites, you can combine the above steps to understand the Standard Transaction Process

<img src="https://i.imgur.com/Hmy74gN.jpg" title="source: imgur.com" />

**Step: 2** the first - the vehicle **needs** to send a SELECT command to obtain the applet protocol version list on the Mobile Phone (HCE).
```
SELECT Command(A000000809434343444B417631h)
```
**Step: 4 & 5**: then, the vehicle inits a standard transaction request by sending AUTH0 Command. 
```
// Vehicle send Auth0 command
AUTH0 Command(applet_ver | vehicle_ePk | transaction_identifier | vehicle_identifier)
```
At the same time, the vehicle needs to generate a temporary KeyPair(vehicle_ePk & vehicle_eSk), the transaction identifier is randomly generated when initiating a standard transaction request, and the vehicle identifier is the unique number of the vehicle. The vehicle end prepares these parameter data and sends it to the mobile phone through the AUTH0 command.

**Step: 6 & 7** is that the Mobile Phone also generates its own temporary KeyPair(endpoint_ePk & endpoint_eSk), which is given to the Vehicle when the AUTH0 command is responded, which is equivalent to the vehicle and Mobile Phone completing the exchange of information - It's first HandShake Step.

**Step: 8 & 9** is the AUTH1 Command command sent by the Vehicle. The main parameter carried by this command is the signature of the vehicle. It mainly uses the **Vehicle private key** (`not the temporary private key generated previously`) to sign the following content and send it to the Mobile phone(Endpoint)
```
// Vehicle send Auth1 Command
vehicle_sig = vehicle_PK_Sign_to (vehicle_identifier | endpoint_ePK.x | vehicle_ePK.x | transaction_identifier | usage = 415D9569h)

AUTH1 Command(vehicle_sig)
```
**Step: 10**  Mobile Phone Verify the signature vehicle_sig when receives 

**Step: 11** If the Mobile Phone passes the vehicle Signature verification, the Mobile Phone will also use its own permanent Private key - endPoint_sK to sign the following content to obtain the terminal signature:
```
endpoint_sig = (vehicle_identifier | endpoint_ePK.x | vehicle_ePK.x | usage = 4E887B4Ch) 
```
then feedback to the Vehicle.

**Step: 13**  The vehicle receives the signature of the Mobile Phone (endpoint_sig) and also performs the signature verification operation by endPoint_pK. 

If the signature verification is successful, the vehicle and the mobile phone complete the two-way authentication process.

------

**Step: 14 && Step: 15**  The ECDH algorithm can negotiate a shared key, which is a symmetric key (Symmetric Key), without storing the key in advance
```
DHKEY = [Vehicle.eSK * Endpoint.ePK | Transaction_identifier] 
or
DHKEY = [Endpoint.eSK * Vehicle.ePK | Transaction_identifier] 
```
In this way, the Vehicle and the Mobile Phone negotiate a symmetric key KDH **without** key transmission.

**Step: 16 && Step: 17**   The vehicle and the mobile phone can use the symmetric key KDH to discretize the keys Kenc/Kmac/Krmac required to establish a secure channel. 

The information required for discretization (Listing 15-20: AUTH1 Processing-Page 225)
```
info ⟵ cod.vehicle_ePK.x || cod.endpoint_ePK.x || cod.transaction_identifier || interface_byte || cod.flag || “Volatile” || 5Ch || 02h || cod.current_protocol_version
```
which is to input Kdh and info information into the SHA-256 encryption algorithm , the derived 48bytes key can be decomposed into three 16-byte secure channel keys.

**Step: 18 && Step: 19**  is similar to the previous step. In this step, the vehicle and the mobile phone also generate a 32-byte symmetric key Kpersistent. The information required for discrete time 
```
info ⟵ cod.vehicle_ePK.x || cod.endpoint_ePK.x || cod.transaction_identifier || interface_byte || cod.flag || “Persistent” || 5Ch || 02h || cod.current_protocol_version
```
which is derived by  inputting Kdh and info information into the SHA-256 encryption algorithm The 32-byte key comes out as Kpersistent. Kpersistent is a long-term symmetric key used to derive encryption keys and session keys. It is stored in the NVM of the vehicle and mobile phone. Kpersisent will not be used in the next secure communication. It is stored and used for fast transactions, so it must go through standard transactions before fast transactions are possible.
```
compute derived_keys according to Listing 15-45 using cod.Kdh, info, keying_material_length
cod.Kenc ⟵ subset of derived_keys at offset 0 with length 16
cod.Kmac ⟵ subset of derived_keys at offset 16 with length 16
cod.Krmac ⟵ subset of derived_keys at offset 32 with length 16
```
```
compute derived_keys according to Listing 15-45 using cod.Kdh, info, keying_material_length
Kpersistent ⟵ subset of derived_keys at offset 0 with length 32
```

**After generating the secure channel key and long-term key Kpersistent, both parties to the transaction can continue related application operations based on the established secure channel, such as mailbox reading and writing, and then end the current transaction. At the same time, because both parties to the transaction generate and save Kpersistent, the two parties can use a fast transaction method to establish a secure channel for the next transaction. That is, after a standard transaction, Kpersistent can be used for quick identity verification later. This is a bit It is similar to the LTK generated after Bluetooth pairing.**

# CCC-Digital-Key-Sample
This project provides
-   NFC card Emulation
-   Digital key by NFC protocol
-   Sample Standard Transaction Flow follow Digital Key CCC
### Note:
>- Some commands of project is based on CCC-TS-101-Digital-Key-R3_1.0.0.pdf, they might out of date, please check out the latest version in below url
>    -   https://carconnectivity.org/digital-key/
>- Currently, UWB is not supported in some regions
>    -   https://issuetracker.google.com/issues/301139099

# Getting started (again and again)
The knowledge you need to know when starting a CCC-Digigal-Key Project, It will help you approach the project easier:
- Communication technology operating over a radio:
    -   NFC
    -   BLE
    -   UWB
- Basic Knowledge of Encryption Standards:
    -   [PKI](https://en.wikipedia.org/wiki/)
    -   [ECDSA](https://en.wikipedia.org/wiki/Elliptic_Curve_Digital_Signature_Algorithm)
    -   [NIST P-256](https://csrc.nist.gov/csrc/media/events/workshop-on-elliptic-curve-cryptography-standards/documents/papers/session6-adalier-mehmet.pdf)
- [Card Emulator](https://developer.android.com/develop/connectivity/nfc/hce)
- Application Protocol Data Unit(APDU): ApduCommand, ApduResponse
    -   [ISO 7816-4](https://en.wikipedia.org/wiki/Smart_card_application_protocol_data_unit)
    -   [TLV(Tag Length Value) data structure](https://en.wikipedia.org/wiki/Type%E2%80%93length%E2%80%93value)
# Abbreviations and acronyms 
Key terms to remember

-   HCE: Host Card Emulation; the NFC communication is routed to the framework instead of the SE
-   SE: Secure Element
-   Device: Endpoint/Mobile App
-   Vehicle: Car/Automotive Device
-   AID: Application Identifier
-   APDU: Application Protocol Data Unit
-   Device Public Key: Also referred to as device.PK and endpoint.PK
-   Device Private Key: Also referred to as device.SK and endpoint.SK
-   Vehicle Private Key: Also referred to as Vehicle.SK and vehicle_sk
-   Vehicle Public Key: Also referred to as Vehicle.PK and vehicle_pk
-   Vehicle.**e**PK <=> Vehicle **Ephemeral** Public key
-   Device.**e**PK <=> Device **Ephemeral** Public key
# Module Structure

<img src="https://i.imgur.com/atdx7Na.png" alt="NFCEmulatorSample" style="width:600px;"/>

# Standard Transaction Flow 
<img src="https://i.imgur.com/sn0tm8u.png" alt="StrandardTransactionFlow" style="width:600px;"/>`

-   Device: 
    -   To be known as: Mobile App(IOS/Android)
    -   In this project, refer: DigitalKey Module
-   Vehicle:
    -   To be known as: Car, Motobike..etc
    -   Refer EmulatorVehicleOem, It's a tool for simulating vehicle

>**The processing related to protocol version checking will be ignored in sample code, focusing only on some important encryption/decryption steps**

>**For simplicity, the following sample code does not use salt and random transaction identifier & vehicle identifier**

## SELECT
Refer 15.3.2.1 SELECT command in CCC-TS-101-Digital-Key-R3_1.0.0.pdf

1. Vehicle(EmulatorVehicleOem) send SELECT Command to Mobile
```kotlin
 val command = CommandApdu( // build ApduRequestCommand
            cla = SELECT.CLA,
            ins = SELECT.INS,
            p1 = 0x04,
            p2 = 0x00,
            data = Config.DIGITAL_KEY_FRAMWORK_AID.hexStringToByteArray(),
            le = 0x00
        )
```
>const val DIGITAL_KEY_FRAMWORK_AID = "A000000809434343444B417631" 
2. Mobile(DigitalKeyModule) build TLV data then return to Vehicle follow structure

| Tag  | Length (bytes) | Description|Field is|
| ------------- | ------------- | ------------- | ------------- |
| 5Ch | variable  |A list of supported Digital Key applet protocol versions ordered from highest to lowest. Each version number is concatenated and encoded on 2 bytes in big-endian notation.  |mandatory  |


3. Vehicle extract data from ApduResponse to prepare next Step

## AUTH0
Allow vehicle to initiate the authen step to
Important steps to be aware of
1. Request: Vehicle send  **Vehicle.ePk** to Device(aka Endpoint/MobileApp)

```kotlin
private fun auth0(): Boolean {
        transactionIdentifier = VehicleIdentity.transaction_identifier
        val payload = BerTlvBuilder()
            .addBytes(BerTag(0x5C), VehicleIdentity.protocol_version)
            .addBytes(
                BerTag(0x87),
                //prepended by 04h
                VehicleIdentity.ephemeralKeyPair.encodedPublicKey // Vehicle.ePk
            )
            .addBytes(BerTag(0x4C), transactionIdentifier)
            .addBytes(BerTag(0x4D), vehicalIdentityValue)
            .buildArray()
        val auth0Command = CommandApdu(
            cla = AUTH0.CLA, // o 84 - in test we used 80
            ins = AUTH0.INS,
            p1 = 0x00,
            p2 = 0x00,
            data = payload,
            le = 0x00
        )
        val byteArray = sendCommandToMobile(command = auth0Command.toBytes())
}
```
2. Response: Device(aka Endpoint/MobileApp) send **Endpoint.ePk** to Vehicle
```kotlin
override fun processCommandApdu(apdu: ByteArray?, extras: Bundle?): ByteArray{
    //...
    val payload = BerTlvBuilder().addBytes(
                    BerTag(0x86),
                    //prepended by 04h
                    devicePublicKey.ephemeralKeyPair.encodedPublicKey) //ephemeral endpoint public key
                    .addText(BerTag(0x9D), TRANSFORMATION).buildArray()
    //...
    return buildSuccessResponse(payload) 
}

```
## AUTH1
1. Request: Vehicle sends **signed data** to device
-   Vehicle Build Payload Data
```kotlin
val auth1Payload = BerTlvBuilder()
         .addBytes(BerTag(0x4D), b0x4D)
        .addBytes(BerTag(0x86), extractPkX(endPointPK))
        .addBytes(BerTag(0x87), extractPkX(vehiclePk))
        .addBytes(BerTag(0x4C), b0x4C) // transaction Identifier
        .addHex(BerTag(0x93), b0x93)//usage = 415D9569h
        .buildArray()
````
-   Vehicle Sign Payload by Vehicle.Sk
```kotlin
val auth1PayloadSigned = Algorithm.getInstance().sign(
            auth1Payload,
            VehicleIdentity.signKeyPair.private as ECPrivateKeyParameters
        )
```
-   Vehicle Transfer Signed data to device
```kotlin
 val auth1Command = CommandApdu(
            cla = AUTH1.CLA,
            ins = AUTH1.INS,
            p1 = 0x00,
            p2 = 0x00,
            data = auth1PayloadSigned,
            le = 0x00
        )
        sendCommandToMobile(command = auth1Command.toBytes())
```
2. Device handles data which is sent by Vehicle
-   Device extract signed payload from 0x9E Tag and check signature by Vehicle.Pk
```kotlin
val tlvs = TlvUtil.parseTLV(apduData)
val dataSignedTlv = tlvs.find(BerTag(0x9E)).bytesValue
Algorithm.getInstance().verify(payloadOriginnal, dataIncomming, vehicleSignPk)
// next step: Device caculateKdhKey by hkdfSha256
```
-   Device build Response Payload 
```kotlin
val responsePayload = BerTlvBuilder().addIntAsHex(BerTag(0x4E), 0, 1) // slot 0 - test just only 1 keypair
                    .addBytes(BerTag(0x9E), endpointSign)
                    .addBytes(BerTag(0x57), byteArrayOf(0x12, 0x34, 0x56, 0x78))// deprecated
                    .addBytes(BerTag(0x4A), byteArrayOf(0x4A)) // confidential_mailbox_data_subset
                    .addBytes(BerTag(0x4B), byteArrayOf(0x4B)) // private mailbox
                    .buildArray()
```
-   The Device encrypts Response Payload by **Vehicle.Pk & Device.Sk**
```kotlin
val sharedSecret = createSharedSecret(vehicle.Pk, device.Sk)
val kdf = KDF2BytesGenerator(SHA256Digest())
kdf.init(KDFParameters(sharedSecret, IV))
val derivedKey = ByteArray(32) // AES-256
kdf.generateBytes(derivedKey, 0, derivedKey.size)
val cipher = cipher()
cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derivedKey, "AES"))
val encryptedPayload = ccipher.doFinal(responsePayload)
// then send encryptedPayload to Vehicle
```
3. Vehicle extracts package
- The vehicle decrypts the encrypted payload it receives by **Vehicle.Sk & Device.pk**
```kotlin
val sharedSecret = createSharedSecret(Device.pk, Vehicle.Sk)
val kdf = KDF2BytesGenerator(SHA256Digest())
kdf.init(KDFParameters(sharedSecret, IV))
val derivedKey = ByteArray(32) // AES-256
kdf.generateBytes(derivedKey, 0, derivedKey.size)
val cipher = cipher()
cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derivedKey, "AES"))
val decryptedPayload = cipher.doFinal(encryptedPayload)
```
-   Packet decryption successful - secure handshake established.
# Environment Setup

-   Mac Mini M1
-   Android Studio Jellyfish | 2023.3.1 Canary 13 (or upper)
-   Android Compile SDK: 34
-   JvmToolchain 17 (or upper)
-   Android Plugin : 8.4.0-alpha13 (or upper)
    -   Gradle Distribution url: https://services.gradle.org/distributions/gradle-8.6-bin.zip
-   Kotlin: 1.8.0 (or upper)  - kotlin-jvm: 1.9.20 (or upper)
# Source Code
TBD
## License
```
   Copyright 2023 baka3k

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
