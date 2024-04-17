package hi.baka3k.security.ec

/**
 * CCC-TS-101-Digital-Key-R3_V1.1.0
 * Table 14-1: Authentication and Privacy Keys
 * */
object ECKeyUtils {
    const val TAG = "Key"
    const val Vehicle_OEM_Sig_SK = "VehicleOEM.Sig.SK"
    const val Vehicle_OEM_Sig_PK = "VehicleOEM.Sig.PK"

    const val Vehicle_OEM_Enc_SK = "VehicleOEM.enc.SK"
    const val Vehicle_OEM_Enc_PK = "VehicleOEM.Enc.PK"

    const val Devx_Enc_PK = "Devx.Enc.PK"
    const val Devx_Enc_SK = "Devx.Enc.SK"
    const val Devx_Sig_PK = "Devx.Sign.PK"
    const val Devx_Sig_SK = "Devx.Sign.SK"
}