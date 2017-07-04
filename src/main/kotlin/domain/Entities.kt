package domain

import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.stripe.model.Charge
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import java.math.BigDecimal
import java.util.*

class Receipt(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Receipt>(Receipts) {
        fun withStripe(result: Charge): Receipt {
            return new {
                type = "stripe"
                data = Gson().toJson(result)
            }
        }
    }

    var type by Receipts.type
    var data by Receipts.data
}

class Client(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Client>(Clients)

    var slug by Clients.slug
    var locale by Clients.locale
    var name by Clients.name
    var details by Clients.details
    var businessNumber by Clients.businessNumber

    fun toJson() = jsonObject(
            "slug" to slug,
            "locale" to locale,
            "name" to name,
            "details" to details,
            "businessNumber" to businessNumber
    )
}

class Provider(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Provider>(Providers)

    var name by Providers.name
    var details by Providers.details
    var businessNumber by Providers.businessNumber
    val contacts by ProviderContact referrersOn Contacts.parent
    val paymentMethods by ProviderPaymentMethod referrersOn PaymentMethods.parent
    var imageUrl by Providers.imageUrl
    var stripePublishableKey by Providers.stripePublishableKey
    var stripeSecretKey by Providers.stripeSecretKey

    fun toJson() = jsonObject(
            "name" to name,
            "details" to details
    )
}

class ProviderPaymentMethod(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ProviderPaymentMethod>(PaymentMethods)

    var name by PaymentMethods.name
    var title by PaymentMethods.title
    var details by PaymentMethods.details
    var parent by Provider referencedOn PaymentMethods.parent

    fun isStripe() = if (name == "Stripe") { name } else { null }
    val foo: String get() = "YEAH ${name} ${isStripe()}"
}

class InvoicePaymentMethod(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<InvoicePaymentMethod>(InvoicePaymentMethods)

    var method by ProviderPaymentMethod referencedOn InvoicePaymentMethods.method
    var invoice by Invoice referencedOn InvoicePaymentMethods.invoice
}

class ProviderContact(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ProviderContact>(Contacts)

    var type by Contacts.type
    var value by Contacts.value
    var parent by Provider referencedOn Contacts.parent
}

class InvoiceItem(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<InvoiceItem>(InvoiceItems)

    var item by InvoiceItems.item
    var price by InvoiceItems.price
    var quantity by InvoiceItems.quantity
    var taxRate by InvoiceItems.taxRate
    var parent by Invoice referencedOn InvoiceItems.parent

    fun toJson(): JsonObject = jsonObject(
            "item" to item,
            "price" to price.toString(),
            "quantity" to quantity.toString(),
            "taxRate" to taxRate.toString()
    )

    val taxPerUnit: BigDecimal get() = price * taxRate
    val totalTax: BigDecimal get() = taxPerUnit * quantity
    val total: BigDecimal get() = (price * quantity) + totalTax
}

class Invoice(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Invoice>(Invoices) {
        fun findByUuid(uuid: UUID): Invoice? {
            return find { Invoices.uuid eq uuid }.firstOrNull()
        }
    }

    var uuid by Invoices.uuid
    var provider by Provider referencedOn Invoices.provider
    var client by Client referencedOn Invoices.client
    var currency by Invoices.currency
    val items by InvoiceItem referrersOn InvoiceItems.parent
    var receipt by Receipt optionalReferencedOn Invoices.receipt
    val paymentMethods by ProviderPaymentMethod via InvoicePaymentMethods

    var issuedDate by Invoices.issuedDate
    var dueDate by Invoices.dueDate
    var reference by Invoices.reference
    var advice by Invoices.advice
    var remarks by Invoices.remarks

    val total: BigDecimal get() = items
            .map { it.total }
            .reduce { acc, x -> acc + x }

    val hasTax: Boolean get() = items
            .map { it.totalTax }
            .reduce { a, b -> a + b } > BigDecimal.ZERO

    fun toJson(): JsonObject = jsonObject(
            "uuid" to uuid.toString(),
            "provider" to provider.toJson(),
            "client" to client.toJson(),
            "currency" to currency,
            "items" to jsonArray(items.map { it.toJson() })
    )
}