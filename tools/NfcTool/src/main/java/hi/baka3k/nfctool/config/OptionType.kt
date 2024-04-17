package hi.baka3k.nfctool.config

enum class OptionType(val value: Int) {
    // LISTEN A
    // ATQA[0]
    LA_BIT_FRAME_SDD(0x30),

    // ATQA[1]
    LA_PLATFORM_CONFIG(0x31),

    // SAK
    LA_SEL_INFO(0x32),

    // UID
    LA_NFCID1(0x33),

    // LISTEN B
    // PUPI
    LB_NFCID0(0x39),

    // Bytes 6-9 of SENSB
    LB_APPLICATION_DATA(0x3A),

    // Start-Up Frame Guard Time (Protocol byte 1)
    LB_SFGI(0x3B),

    // Max Frames (128 bytes) / Protocol Type ISO-DEP support (Protocol byte 2)
    LB_SENSB_INFO(0x38),

    // FWI / ADC / F0 (Protocol byte 3)
    LB_ADC_FO(0x3C),


    // LISTEN F
    // contains [0:2] SystemCode and [3:10] NFCID2
    LF_T3T_IDENTIFIERS_1(0x40),

    // bitmask of valid T3T_IDENTIFIERS
    LF_T3T_FLAGS(0x53),

    // "manufacturer" aka PAD0, PAD1, MRTI_check, MRTI_update, PAD2
    LF_T3T_PMM(0x51),

    // LISTEN ISO-DEP
    // Historical bytes (NCI spec calls this LI_A_HIST_BY)
    LA_HIST_BY(0x59),

    // Higher layer response field
    LB_H_INFO_RSP(0x5A),
    ;

    open fun getID(): Byte {
        return value.toByte()
    }

    companion object {
        fun fromType(type: Byte): OptionType? {
            for (optionType in values()) {
                if (optionType.getID() == type){
                    return optionType
                }
            }
            return null
        }
    }
}