package it.pureorigins.velocityconfiguration

import freemarker.core.CommonMarkupOutputFormat
import freemarker.core.CommonTemplateMarkupOutputModel
import freemarker.template.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.io.StringWriter
import java.io.Writer
import java.time.*
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.random.Random

private fun String.template(args: Map<String, Any?>, configuration: Configuration.() -> Unit): String {
    val reader = reader()
    val writer = StringWriter()
    val configuration = Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).apply {
        registeredCustomOutputFormats = listOf(JsonOutputFormat)
        whitespaceStripping = false
        logTemplateExceptions = false
        objectWrapper = ObjectWrapper
        outputEncoding = "utf8"
        isAPIBuiltinEnabled = true
        setSharedVariable("unicode", UnicodeTemplateMethodModel)
        setSharedVariable("random", Random)
        configuration()
    }
    val template = Template("Configuration", reader, configuration)
    template.process(args, writer)
    return writer.toString()
}

fun String.template(args: Map<String, Any?>, locale: Locale = Locale.ROOT) = template(args) {
    this.locale = locale
}

fun String.template(vararg args: Pair<String, Any?>, locale: Locale = Locale.ROOT) = template(args.toMap(), locale)

fun String.templateComponent(args: Map<String, Any?>, locale: Locale = Locale.ROOT) = if (startsWith('{') || startsWith('[')) {
    GsonComponentSerializer.gson().deserialize(
        template(args) {
            this.locale = locale
            outputFormat = JsonOutputFormat
        }
    )
} else Component.text(template(args, locale))

fun String.templateComponent(vararg args: Pair<String, Any?>, locale: Locale = Locale.ROOT) = templateComponent(args.toMap(), locale)

private class TemplateJsonOutputModel(plainTextContent: String?, markupContent: String?) : CommonTemplateMarkupOutputModel<TemplateJsonOutputModel>(plainTextContent, markupContent) {
    override fun getOutputFormat() = JsonOutputFormat
}

private object JsonOutputFormat : CommonMarkupOutputFormat<TemplateJsonOutputModel>() {
    override fun getName() = "Markdown"
    override fun getMimeType() = null
    override fun output(textToEsc: String, out: Writer) {
        textToEsc.forEach {
            when (it) {
                '\\', '"' -> out.write("\\$it")
                else -> out.write(it.code)
            }
        }
    }
    override fun escapePlainText(plainTextContent: String) = buildString {
        plainTextContent.forEach {
            when (it) {
                '\\', '"' -> append("\\$it")
                else -> append(it)
            }
        }
    }
    override fun isLegacyBuiltInBypassed(builtInName: String) = false
    override fun newTemplateMarkupOutputModel(plainTextContent: String?, markupContent: String?) = TemplateJsonOutputModel(plainTextContent, markupContent)
}

private object UnicodeTemplateMethodModel : TemplateMethodModelEx {
    override fun exec(args: MutableList<Any?>): Any {
        if (args.size != 1) throw TemplateModelException("Wrong arguments")
        return SimpleScalar((args[0] as SimpleNumber).asNumber.toChar().toString())
    }
}

private object ObjectWrapper : DefaultObjectWrapper(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS) {
    override fun wrap(obj: Any?): TemplateModel? = when (obj) {
        is Int -> SimpleNumber(obj)
        is Long -> SimpleNumber(obj)
        is Byte -> SimpleNumber(obj)
        is Short -> SimpleNumber(obj)
        is Double -> SimpleNumber(obj)
        is Float -> SimpleNumber(obj)
        is LocalDateTime -> SimpleDate(Date.from(obj.toInstant(ZoneOffset.UTC)), TemplateDateModel.DATETIME)
        is LocalDate -> SimpleDate(Date.from(obj.atStartOfDay().toInstant(ZoneOffset.UTC)), TemplateDateModel.DATE)
        is LocalTime -> SimpleDate(Date.from(obj.atDate(LocalDate.ofEpochDay(0)).toInstant(ZoneOffset.UTC)), TemplateDateModel.TIME)
        is Instant -> SimpleDate(Date.from(obj), TemplateDateModel.DATETIME)
        is ZonedDateTime -> SimpleDate(Date.from(obj.toInstant()), TemplateDateModel.DATETIME)
        is OffsetDateTime -> SimpleDate(Date.from(obj.toInstant()), TemplateDateModel.DATETIME)
        is OffsetTime -> SimpleDate(Date.from(obj.atDate(LocalDate.ofEpochDay(0)).toInstant()), TemplateDateModel.TIME)
        is LinkedHashMap<*, *> -> DefaultMapAdapter.adapt(obj, this)
        else -> super.handleUnknownType(obj)
    }
}