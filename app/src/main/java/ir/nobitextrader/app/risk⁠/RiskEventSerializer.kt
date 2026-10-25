
package ir.nobitextrader.app.risk

/**
 * Converts risk events to a stable, dependency-free record format.
 *
 * This serializer does not write files or database records.
 * Persistent storage will be implemented separately.
 */
object RiskEventSerializer {

    private const val SEPARATOR = '\t'
    private const val NULL_VALUE = "\\N"

    fun serialize(event: RiskEvent): String {
        val fields = listOf(
            event.type.name,
            event.timestampMillis.toString(),
            encode(event.message),
            encodeNullable(event.equityUsd),
            encodeNullable(event.drawdownPct),
            encodeNullable(event.dailyLossPct),
            encodeNullable(event.symbol)
        )

        return fields.joinToString(SEPARATOR.toString())
    }

    fun deserialize(record: String): RiskEvent {
        val fields = record.split(SEPARATOR)

        require(fields.size == 7) {
            "Invalid risk event record: expected 7 fields"
        }

        val type = try {
            RiskEventType.valueOf(fields[0])
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Unknown risk event type",
                exception
            )
        }

        val timestamp = fields[1].toLongOrNull()
            ?: throw IllegalArgumentException("Invalid timestamp")

        val message = decode(fields[2])

        val equity = decodeNullableDouble(fields[3])
        val drawdown = decodeNullableDouble(fields[4])
        val dailyLoss = decodeNullableDouble(fields[5])
        val symbol = decodeNullable(fields[6])

        return RiskEvent(
            type = type,
            timestampMillis = timestamp,
            message = message,
            equityUsd = equity,
            drawdownPct = drawdown,
            dailyLossPct = dailyLoss,
            symbol = symbol
        )
    }

    private fun encode(value: String): String {
        return value
            .replace("%", "%25")
            .replace("\t", "%09")
            .replace("\n", "%0A")
            .replace("\r", "%0D")
    }

    private fun decode(value: String): String {
        return value
            .replace("%0D", "\r")
            .replace("%0A", "\n")
            .replace("%09", "\t")
            .replace("%25", "%")
    }

    private fun encodeNullable(value: String?): String {
        return value?.let(::encode) ?: NULL_VALUE
    }

    private fun encodeNullable(value: Double?): String {
        return value?.toString() ?: NULL_VALUE
    }

    private fun decodeNullable(value: String): String? {
        return if (value == NULL_VALUE) null else decode(value)
    }

    private fun decodeNullableDouble(value: String): Double? {
        if (value == NULL_VALUE) return null

        val number = value.toDoubleOrNull()
            ?: throw IllegalArgumentException("Invalid numeric field")

        require(number.isFinite() && number >= 0.0) {
            "Numeric field must be finite and non-negative"
        }

        return number
    }
}
