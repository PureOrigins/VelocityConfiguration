package it.pureorigins.velocityconfiguration

import kotlinx.serialization.*
import java.io.IOException
import java.nio.file.Path
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.lang.Thread.currentThread
import kotlin.io.path.*
import kotlin.reflect.typeOf

val compactJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

val json = Json(compactJson) {
    prettyPrint = true
}

fun serializationException(file: Path, cause: SerializationException): Nothing = throw SerializationException("Cannot serialize file '$file'", cause)
fun deserializationException(file: Path, cause: SerializationException): Nothing = throw SerializationException("Cannot deserialize file '$file' (update or delete it)", cause)
fun ioException(file: Path, cause: IOException): Nothing = throw IOException("Cannot access file '$file'", cause)
fun readException(file: Path, cause: Throwable): Nothing = when (cause) {
    is SerializationException -> deserializationException(file, cause)
    is IOException -> ioException(file, cause)
    else -> throw IOException("Cannot read file '$file'", cause)
}
fun writeException(file: Path, cause: Throwable): Nothing = when (cause) {
    is SerializationException -> serializationException(file, cause)
    is IOException -> ioException(file, cause)
    else -> throw IOException("Cannot write file '$file'", cause)
}

inline fun <reified T> Json.readFileAs(file: Path, deserializer: DeserializationStrategy<T> = serializersModule.serializer(), crossinline exceptionHandler: (Throwable) -> T = { readException(file, it) }): T {
    val text = try {
        file.readText()
    } catch (e: Throwable) {
        return exceptionHandler(e)
    }
    return try {
        decodeFromString(deserializer, text)
    } catch (e: Throwable) {
        @OptIn(ExperimentalStdlibApi::class)
        exceptionHandler(SerializationException("An error occurred while deserializing $text to ${typeOf<T>()}", e))
    }
}

inline fun <reified T> Json.readFileAs(
    file: Path,
    default: T,
    serializer: KSerializer<T> = serializersModule.serializer(),
    crossinline writeExceptionHandler: (Throwable) -> Unit = { writeException(file, it) },
    crossinline exceptionHandler: (Throwable) -> T = { readException(file, it) }
): T {
    return if (!file.exists()) {
        writeFile(file, default, serializer, writeExceptionHandler)
        default
    } else {
        readFileAs(file, serializer, exceptionHandler)
    }
}

inline fun <reified T> Json.readFileOrCopy(
    file: Path,
    defaultPath: String,
    deserializer: DeserializationStrategy<T> = serializersModule.serializer(),
    classLoader: ClassLoader = currentThread().contextClassLoader,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file, it) }
): T {
    if (!file.exists()) {
        try {
            file.parent?.createDirectories()
            classLoader.getResourceAsStream("config/$defaultPath")?.use {
                it.copyTo(file.outputStream())
            } ?: throw FileNotFoundException("cannot find resource on 'config/$defaultPath'")
        } catch (e: Throwable) {
            return exceptionHandler(e)
        }
    }
    return readFileAs(file, deserializer, exceptionHandler)
}

inline fun <reified T> Json.writeFile(
    file: Path,
    content: T,
    serializer: SerializationStrategy<T> = serializersModule.serializer(),
    crossinline exceptionHandler: (Throwable) -> Unit = { writeException(file, it) }
) {
    val text = try {
        encodeToString(serializer, content)
    } catch (e: Throwable) {
        return exceptionHandler(SerializationException("An error occurred while serializing $content", e))
    }
    try {
        file.parent?.createDirectories()
        file.writeText(text)
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}
