package homework03.serializer

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

@Target(AnnotationTarget.PROPERTY)
annotation class CsvIgnore

fun <T : Any> csvSerialize(data: Iterable<T>, klass: KClass<T>) = buildString { serializeObject(data, klass) }

private fun <T : Any> StringBuilder.serializeObject(data: Iterable<T>, klass: KClass<T>) {
    serializeHeader(klass)
    data.forEach {
        serializeObject(it)
    }
}

private fun StringBuilder.serializeNumber(value: Number) = apply {
    append(value)
}

private fun StringBuilder.serializeValue(value: Any) = apply {
    when (value.javaClass.kotlin) {
        String::class -> {
            serializeString(value as String)
        }
        Integer::class, Short::class, Long::class, Byte::class, Float::class, Double::class -> {
            serializeNumber(value as Number)
        }
    }
}

private fun StringBuilder.serializeString(value: String) = apply {
    append('"')
    append(value.replace("\"", "\"\""))
    append('"')
}

private fun <T : Any> StringBuilder.serializeHeader(klass: KClass<T>) = apply {
    val properties = klass.memberProperties.filter { it.findAnnotation<CsvIgnore>() == null }
    properties.joinTo(this, ",") { p ->
        serializeString(p.name)
        ""
    }
    append("\n")
}

private fun StringBuilder.serializeObject(value: Any) {
    val kClass = value.javaClass.kotlin
    val properties = kClass.memberProperties.filter { it.findAnnotation<CsvIgnore>() == null }
    properties.joinTo(this, ",") { p ->
        serializeValue(p.get(value) ?: "")
        ""
    }
    append("\n")
}
