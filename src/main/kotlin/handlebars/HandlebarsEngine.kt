package handlebars

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.MarkdownHelper
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.cache.GuavaTemplateCache
import com.github.jknack.handlebars.helper.StringHelpers
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.github.jknack.handlebars.io.TemplateSource
import com.google.common.cache.CacheBuilder
import domain.Invoice
import org.eclipse.jetty.io.RuntimeIOException
import spark.ModelAndView
import java.io.IOException
import java.util.concurrent.TimeUnit

class HandlebarsEngine(resourceRoot: String = "/templates") : Handlebars() {
    init {
        val cache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build<TemplateSource, Template>()

        with(ClassPathTemplateLoader(resourceRoot, null))
        with(GuavaTemplateCache(cache))

        registerHelper("markdown", MarkdownHelper())
        registerHelper("ifEquals", Helper<Any> { a, opts ->
            val b = opts.param<Any>(0)
            if (a == b) {
                opts.fn()
            } else {
                opts.inverse()
            }
        })
        registerHelper("stripe", Helper<Invoice> { invoice, _ ->
            SafeString("<a href=\"/payment/${invoice.uuid}\">" +
                    "Pay by card" +
                    "</a><br><small>(Powered by <a href=\"https://stripe.com\">Stripe</a>)</small>")
        })
        registerHelpers(FormatHelpers)
    }

    fun render(modelAndView: ModelAndView): String {
        val viewName = modelAndView.viewName
        try {
            val template = compile(viewName)
            return template.apply(modelAndView.model)
        } catch (e: IOException) {
            throw RuntimeIOException(e)
        }
    }

    fun render(model: Map<Any, Any>?, viewName: String): String {
        val v = if (viewName.endsWith(".hbs")) {
            viewName
        } else {
            "$viewName.hbs"
        }

        return render(ModelAndView(model, v))
    }
}