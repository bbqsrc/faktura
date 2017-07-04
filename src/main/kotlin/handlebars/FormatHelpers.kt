package handlebars

import com.github.jknack.handlebars.Options
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.text.NumberFormat
import java.util.*

/**
 * Created by brendan on 4/7/17.
 */

object FormatHelpers {
    fun formatDate(date: DateTime?, pattern: String): String {
        if (date == null) {
            return "NO DATE"
        }

        val style = when (pattern) {
            "short" -> "S-"
            "medium" -> "M-"
            "long" -> "L-"
            "full" -> "F-"
            else -> throw IllegalArgumentException(pattern)
        }

        return DateTimeFormat.forStyle(style).print(date.toInstant())
    }

    private fun formatCurrency(number: Any, options: Options): String {
        val currency = options.hash<String>("currency") ?: throw IllegalArgumentException("Currency missing")

        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        formatter.currency = Currency.getInstance(currency)
        try {
            return formatter.format(number)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("${number} cannot be formatted")
        }

    }

    fun formatNumber(number: Any, options: Options): String {
        val style = options.hash<String>("style")

        return when (style) {
            "currency" -> formatCurrency(number, options)
            else -> NumberFormat.getNumberInstance().format(number)
        }
    }
}