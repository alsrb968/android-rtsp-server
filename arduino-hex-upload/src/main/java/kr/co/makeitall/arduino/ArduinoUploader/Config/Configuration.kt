package kr.co.makeitall.arduino.ArduinoUploader.Config

data class Configuration(
    var arduinos: Array<Arduino> = arrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Configuration

        if (!arduinos.contentEquals(other.arduinos)) return false

        return true
    }

    override fun hashCode(): Int {
        return arduinos.contentHashCode()
    }
}
