package domain

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.StringColumnType
import org.jetbrains.exposed.sql.Table

fun Table.clob(name: String): Column<String> = registerColumn(name, StringColumnType(256))

object Receipts : IntIdTable("receipts") {
    val type = text("type")
    val data = clob("data")
}

object Clients : IntIdTable("clients") {
    val slug = varchar("slug", 255).index()
    val locale = varchar("locale", 255)
    val name = text("name")
    val details = text("details")
    val businessNumber = text("business_number").nullable()
}

object PaymentMethods : IntIdTable("provider_payment_methods") {
    val name = varchar("name", 255)
    val title = text("title")
    val details = text("details")
    val parent = reference("provider_id", Providers)
}

object InvoicePaymentMethods : IntIdTable("invoice_payment_methods") {
    val method = reference("payment_method_id", PaymentMethods).index()
    val invoice = reference("invoice_id", Invoices).index()
}

object Contacts : IntIdTable("provider_contacts") {
    val type = text("type")
    val value = text("value")
    val parent = reference("provider_id", Providers)
}

object Providers : IntIdTable("providers") {
    val name = varchar("name", 255).index()
    val details = text("details").nullable()
    val businessNumber = text("business_number")
    val imageUrl = text("image_url").nullable()
    val stripePublishableKey = text("stripe_publishable_key").nullable()
    val stripeSecretKey = text("stripe_secret_key").nullable()
    //val paymentMethods = reference("payment_methods", PaymentMethods)
    //val contacts = reference("contacts", Contacts)
}

object InvoiceItems : IntIdTable("invoice_items") {
    val item = text("item")
    val price = decimal("price", 10, 4)
    val quantity = decimal("quantity", 10, 4)
    val taxRate = decimal("tax_rate", 10, 4)
    val parent = reference("invoice_id", Invoices)
}

object Invoices : IntIdTable("invoices") {
    val provider = reference("provider_id", Providers)
    val client = reference("client_id", Clients)
    val currency = text("currency")
    val issuedDate = datetime("issued_date").nullable()
    val dueDate = datetime("due_date").nullable()
    val reference = text("reference").nullable()
    val advice = text("advice").nullable()
    val remarks = text("remarks").nullable()
    val uuid = uuid("uuid").uniqueIndex()
    val receipt = reference("receipt_id", Receipts).nullable()
}