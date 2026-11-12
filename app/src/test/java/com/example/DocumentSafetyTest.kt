package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.CharacterCodingException

class DocumentSafetyTest {
    @Test fun acceptsExactLimitButRejectsNextByteBeforeReadingRest() {
        assertEquals("abcd", readDocumentText(ByteArrayInputStream("abcd".toByteArray()), 4))
        var reads = 0
        val untrusted = object : InputStream() {
            override fun read(): Int { reads++; return 'a'.code }
        }
        assertThrows(IllegalArgumentException::class.java) { readDocumentText(untrusted, 4) }
        assertEquals(5, reads)
    }

    @Test fun acceptsUtf8AndBomAndRejectsMalformedInput() {
        assertEquals("水 café", readDocumentText(ByteArrayInputStream("\uFEFF水 café".toByteArray())))
        assertThrows(CharacterCodingException::class.java) {
            readDocumentText(ByteArrayInputStream(byteArrayOf(0xC3.toByte(), 0x28)))
        }
    }

    @Test fun rejectsHostileNestingAndTruncationButIgnoresBracesInsideStrings() {
        validateDocumentStructure("{\"note\":\"[{}] and \\\"quoted\\\"\",\"items\":[]}")
        validateDocumentStructure("[".repeat(64) + "]".repeat(64))
        assertThrows(IllegalArgumentException::class.java) { validateDocumentStructure("[".repeat(65) + "]".repeat(65)) }
        assertThrows(IllegalArgumentException::class.java) { validateDocumentStructure("{\"note\":\"unfinished") }
        assertThrows(IllegalArgumentException::class.java) { validateDocumentStructure("}") }
    }
}
