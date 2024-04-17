package hi.baka3k.nfctool.config


class ConfigBuilder {
    private val mOptions: MutableList<ConfigOption> = mutableListOf()

    constructor()
    constructor(config: ByteArray) {
        parse(config)
    }

    fun build(): ByteArray {
        var length = 0
        for (option in mOptions) length += option.size() + 2
        val data = ByteArray(length)
        var offset = 0
        for (option in mOptions) {
            option.push(data, offset)
            offset += option.size() + 2
        }
        return data
    }

    private fun parse(config: ByteArray) {
        mOptions.clear()
        var index = 0
        while (index + 2 < config.size) {
            val type = config[index + 0]
            val length = config[index + 1]
            val data = ByteArray(length.toInt())
            System.arraycopy(config, index + 2, data, 0, length.toInt())
            val optionType = OptionType.fromType(type)
            if (optionType != null) {
                add(optionType, data)
            }
            index += length + 2
        }
    }

    fun add(ID: OptionType, data: ByteArray) {
        mOptions.add(ConfigOption(ID, data))
    }

    fun add(ID: OptionType, data: Byte) {
        mOptions.add(ConfigOption(ID, data))
    }

    fun add(option: ConfigOption) {
        mOptions.add(option)
    }

    fun getOptions(): List<ConfigOption> {
        return mOptions
    }
}