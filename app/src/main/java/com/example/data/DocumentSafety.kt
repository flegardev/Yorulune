package com.example.data

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

const val MAX_DOCUMENT_BYTES = 16 * 1024 * 1024

/** Never trust a provider's reported size. Limit bytes while streaming, before decoding. */
fun readDocumentText(input: InputStream, limit: Int = MAX_DOCUMENT_BYTES): String {
    require(limit in 1..MAX_DOCUMENT_BYTES)
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer, 0, minOf(buffer.size, limit - output.size() + 1))
        if (count < 0) break
        if (count == 0) continue
        require(output.size() + count <= limit) { "File exceeds the 16 MiB safe import limit." }
        output.write(buffer, 0, count)
    }
    return Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(output.toByteArray())).toString().removePrefix("\uFEFF")
}

/** Bound pasted input and JSON nesting before invoking the platform parser. */
fun validateDocumentStructure(text: String) {
    require(text.length <= MAX_DOCUMENT_BYTES && text.toByteArray(Charsets.UTF_8).size <= MAX_DOCUMENT_BYTES) {
        "File exceeds the 16 MiB safe import limit."
    }
    var depth = 0
    var quoted = false
    var escaped = false
    text.forEach { c ->
        if (quoted) {
            if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == '"') quoted = false
        } else when (c) {
            '"' -> quoted = true
            '{', '[' -> { depth++; require(depth <= 64) { "Backup nesting exceeds the safe limit." } }
            '}', ']' -> { depth--; require(depth >= 0) { "Unbalanced backup document." } }
        }
    }
    require(!quoted && depth == 0) { "Incomplete backup document." }
}
