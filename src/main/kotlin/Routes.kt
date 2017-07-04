import com.stripe.model.Charge
import com.stripe.net.RequestOptions
import domain.Invoice
import domain.Receipt
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.transactions.transaction
import spark.kotlin.Http
import spark.kotlin.halt
import spark.kotlin.ignite
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import javax.sql.DataSource

internal val hbs = HandlebarsEngine()

internal fun calculateStripeFee(goal: BigDecimal): BigDecimal {
    val fixed = BigDecimal("0.30")
    val percent = BigDecimal("0.029")

    return (goal + fixed) * percent
}

internal fun generateStripeTotal(total: BigDecimal): String {
    return (total * BigDecimal(100))
            .setScale(0, RoundingMode.UP)
            .toBigInteger()
            .toString()
}

internal fun BigDecimal.toCurrencyString(): String {
    return this.setScale(2, RoundingMode.UP).toString()
}

internal fun uuidFromString(string: String): UUID? {
    return try {
        UUID.fromString(string)
    } catch (e: IllegalArgumentException) {
        null
    }
}

internal fun Invoice.toTemplateMap(): Map<Any, Any> {
    val fee = calculateStripeFee(total)
    val stripeTotal = generateStripeTotal(total + fee)

    return mapOf(
            "uuid" to uuid,
            "currency" to currency,
            "stripeTotal" to stripeTotal,
            "fee" to fee.setScale(4, RoundingMode.UP).toString(),
            "amount" to total.toCurrencyString(),
            "totalPayable" to (total + fee).toCurrencyString(),
            "client" to client,
            "provider" to provider
    )
}

fun initRoutes(pool: DataSource): Http {
    val http = ignite()

    http.get("/invoice/:uuid") {
        val uuid = uuidFromString(request.params(":uuid")) ?: throw halt(400)

        Database.connect(pool)

        transaction {
            val invoice = Invoice.findByUuid(uuid) ?: throw halt(404)

            if (invoice.receipt == null) {
                hbs.render(invoice.toTemplateMap(), "payment")
            } else {
                "This invoice has already been paid."
            }
        }
    }

    http.post("/invoice/:uuid") {
        val uuid = uuidFromString(request.params(":uuid")) ?: throw halt(400)

        val stripeToken = request.queryParams("stripeToken") ?: throw halt(400)
        // val stripeTokenType = request.queryParams("stripeTokenType") ?: throw halt(400)
        val stripeEmail = request.queryParams("stripeEmail") ?: throw halt(400)

        Database.connect(pool)

        val invoice = transaction { Invoice.findByUuid(uuid) } ?: throw halt(404)

        transaction {
            if (invoice.receipt != null) {
                throw halt(400, "This invoice has already been paid.")
            }
        }

        val fee = transaction { calculateStripeFee(invoice.total) }
        val total = transaction { generateStripeTotal(invoice.total + fee) }
        val sk = transaction { invoice.provider.stripeSecretKey } ?: throw halt(500, "Payment processor has not been configured for this user.")

        val reqOpts = RequestOptions.builder()
                .setApiKey(sk)
                .build()

        val result = Charge.create(mapOf(
                "amount" to total,
                "currency" to invoice.currency,
                "description" to "Invoice ${invoice.uuid}",
                "source" to stripeToken,
                "receipt_email" to stripeEmail,
                "metadata" to mapOf("uuid" to uuid)
        ), reqOpts)

        if (result.status == "succeeded") {
            transaction {
                logger.addLogger(StdOutSqlLogger)
                var receipt = Receipt.withStripe(result)
                invoice.receipt = receipt
                invoice.flush()
            }

            hbs.render(null, "payment-success")
        } else {
            hbs.render(null, "payment-failed")
        }
    }

    return http
}