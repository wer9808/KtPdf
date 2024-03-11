package org.example

import java.io.File

class PdfLexer {

    var pdfFile: File? = null
    lateinit var pdfContent: ByteArray
    var fileEnd: Int = 0
    private var current: Int = 0
    lateinit var pdfTokens: ArrayList<PdfToken>

    companion object {
        var debug = false
    }

    constructor() {

    }

    constructor(pdfFile: File) : this() {
        this.pdfFile = pdfFile
        try {
            this.pdfContent = pdfFile.readBytes()
            fileEnd = pdfContent.size
        } catch (e: Exception) {
            this.pdfFile = null
        }

    }

    fun scan() {
        if (this.pdfFile == null) {
            println("Scanning Error: No PDF")
            return
        }

        pdfTokens = arrayListOf()
        current = 0
        var token: PdfToken
        while (current < fileEnd) {
            val byte = pdfContent[current]
            when (getByteType(byte)) {
                ByteType.Whitespace -> {
                    this.current++
                }
                ByteType.NumLiteral -> {
                    token = scanNumLiteral()
                    pdfTokens.add(token)
                }
                ByteType.AnnotationLiteral -> {
                    token = scanAnnotationLiteral()
                    pdfTokens.add(token)
                }
                ByteType.NameLiteral -> {
                    token = scanNameLiteral()
                    pdfTokens.add(token)
                }
                ByteType.Keyword -> {
                    token = scanKeyword()
                    pdfTokens.add(token)
                }
                ByteType.Wrapper -> {
                    token = scanWrapper()
                    pdfTokens.add(token)
                }
                ByteType.Operator -> {
                    // +
                    if (byte.toInt() == 43) {
                        current++
                        token = scanNumLiteral()
                        pdfTokens.add(token)
                    }
                    // -
                    if (byte.toInt() == 45) {
                        current++
                        token = scanNumLiteral().apply { isNegative = true }
                        pdfTokens.add(token)
                    }
                }
                else -> {
                    if (PdfLexer.debug) {
                        printTokens(0)
                    }
                    throw Error("Scanning Error : Unknown ByteType\n" +
                            "${current}:${Integer.toBinaryString(byte.toInt())}")
                }
            }
        }
        pdfTokens.add(PdfToken(PdfToken.Kind.EndOfToken, null))

        return
    }

    fun printToken(token: PdfToken) {
        if (token.content != null &&
            token.content.find { x -> x.toInt() > 127 || x.toInt() < 0 } == null) {
            val str = String(token.content.toByteArray())
            print("${token.kind} : $str || ")
        }
        else if (token.kind == PdfToken.Kind.Stream) {
            print("${token.kind} : ${token.content!!.size} ||")
        }
        else print("${token.kind} : ${token.content} ||")
    }

    fun printTokens(mode: Int) {
        if (mode == 0) {
            for (i in 0..pdfTokens.size - 1 step(10)) {
                val limit = if (i + 9 < pdfTokens.size) 10 else pdfTokens.size - i
                for (j in 0..limit - 1) {
                    val token = pdfTokens[i + j]
                    printToken(token)
                }
                println()
            }
        }
        else {
            for (token in pdfTokens) {
                printToken(token)
            }
        }
    }

    fun parse() {

        return
    }

    fun scanNumLiteral(): PdfToken {
        val content = arrayListOf<Byte>()
        var byte = pdfContent[current]
        var real = false
        while (current < fileEnd && isByteType(byte, ByteType.NumLiteral)) {
            content.add(byte)
            byte = pdfContent[++current]
        }
        if (byte.toInt() == '.'.code) { // If Real Number
            do {
                real = true
                content.add(byte)
                byte = pdfContent[++current]
            } while (current < fileEnd && isByteType(byte, ByteType.NumLiteral))
        }
        return PdfToken(PdfToken.Kind.Number, content).apply {
            isReal = real
        }
    }

    fun scanNameLiteral(): PdfToken {
        val content = arrayListOf<Byte>()
        var byte = pdfContent[++current]
        while (current < fileEnd && isByteType(byte, ByteType.NameLiteral)) {
            if (content.size > 127) {
                throw Error("Scanning Error : too long name\n" +
                        "${current}:${content}")
            }
            content.add(byte)
            byte = pdfContent[++current]
        }
        return PdfToken(PdfToken.Kind.Name, content)
    }

    fun scanAnnotationLiteral(): PdfToken {
        val content = arrayListOf<Byte>()
        var byte = pdfContent[++current]
        while (current < fileEnd && isByteType(byte, ByteType.AnnotationLiteral)) {
            content.add(byte)
            byte = pdfContent[++current]
        }
        return PdfToken(PdfToken.Kind.Annotation, content)
    }

    fun scanKeyword(): PdfToken {
        var content: ArrayList<Byte>? = arrayListOf<Byte>()
        var byte = pdfContent[current]
        var kind = PdfToken.Kind.Keyword
        while (current < fileEnd && isByteType(byte, ByteType.Keyword)) {
            content!!.add(byte)
            byte = pdfContent[++current]
        }
        while (current < fileEnd && isByteType(byte, ByteType.Whitespace)) {
            byte = pdfContent[++current]
        }
        val contentBytes = content!!.toByteArray()
        if (contentBytes.contentEquals("stream".toByteArray())) { // check stream
            return scanStream()
        }
        else if (contentBytes.contentEquals("obj".toByteArray())) {
            kind = PdfToken.Kind.StartObject
            content = null
        }
        else if (contentBytes.contentEquals("endobj".toByteArray())) {
            kind = PdfToken.Kind.EndObject
            content = null
        }
        else if (contentBytes.contentEquals("R".toByteArray())) {
            kind = PdfToken.Kind.Indirect
            content = null
        }
        else if (contentBytes.contentEquals("true".toByteArray())) {
            kind = PdfToken.Kind.True
        }
        else if (contentBytes.contentEquals("false".toByteArray())) {
            kind = PdfToken.Kind.False
        }
        else if (contentBytes.contentEquals("null".toByteArray())) {
            kind = PdfToken.Kind.Null
        }
        return PdfToken(kind, content)
    }

    fun scanStream(): PdfToken {
        val content = arrayListOf<Byte>()
        var byte = pdfContent[++current]
        while (true) {
            if (current + 8 >= fileEnd) break
            if (byte.toInt() == 101) {
                if (pdfContent.sliceArray(current..current + 8)
                        .contentEquals("endstream".toByteArray())) {
                    break
                }
            }
            content.add(byte)
            byte = pdfContent[++current]
        }
        current += 9

        val token = PdfToken(PdfToken.Kind.Stream, content)
        return token
    }

    fun scanWrapper(): PdfToken {
        var byte = pdfContent[current]
        var kind = PdfToken.Kind.Unknown

        when (byte.toInt()) {
            40 -> { // (
                return scanString()
            }
            60 -> { // <
                if (current + 1 < fileEnd) {
                    byte = pdfContent[++current]
                    if (byte.toInt() == 60) { // dictionary check
                        current++
                        return PdfToken(PdfToken.Kind.StartDict, null)
                    }
                    else {
                        return scanHexString()
                    }
                }
            }
            62 -> { // >
                if (current + 1 < fileEnd) {
                    byte = pdfContent[++current]
                    if (byte.toInt() == 62) {
                        current++
                        return PdfToken(PdfToken.Kind.EndDict, null)
                    }
                }
            }
            91 -> { // [
                current++
                return PdfToken(PdfToken.Kind.LeftBracket, null)
            }
            93 -> { // ]
                current++
                return PdfToken(PdfToken.Kind.RightBracket, null)
            }
        }

        throw Error("Scanning Error : No Matching Wrapper\n" + "${current}:${byte}")
    }


    fun scanOperator() {

    }

    fun scanString(): PdfToken {
        val content = arrayListOf<Byte>()
        var byte = pdfContent[++current]
        while (current < fileEnd && isByteType(byte, ByteType.StrLiteral)) {
            if (byte.toInt() == '\\'.code) {
                byte = pdfContent[++current]
                when (byte.toInt()) {
                    // (
                    40 -> {

                    }
                    // )
                    41 -> {

                    }
                    // \
                    92 -> {
                        byte = 92
                    }
                    // b (BS)
                    98 -> {
                        byte = 8
                    }
                    // f (FF)
                    102 -> {
                        byte = 12
                    }
                    // n (LF)
                    110 -> {
                        byte = 10
                    }
                    // r (CR)
                    114 -> {
                        byte = 13
                    }
                    // t
                    116 -> {
                        byte = 9
                    }
                    else -> {
                        // add octal symbol /
                    }
                }
            }
            content.add(byte)
            byte = pdfContent[++current]
        }
        current++
        return PdfToken(PdfToken.Kind.String, content)
    }

    fun scanHexString(): PdfToken {
        val content = arrayListOf<Byte>()
        var byte = pdfContent[current]
        while (current < fileEnd && isByteType(byte, ByteType.HexStrLiteral)) {
            content.add(byte)
            byte = pdfContent[++current]
        }
        current++
        return PdfToken(PdfToken.Kind.HexString, content)
    }

    fun getByteType(byte: Byte): ByteType {
        if (byte.toInt() < 128) {
            return getCharType(byte)
        }
        if (byte.toInt() >= 128) {
            return ByteType.Bytes
        }
        return ByteType.Unknown
    }

    fun getCharType(byte: Byte): ByteType {
        if (byte in PdfToken.ASCII_WHITESPACE) {
            return ByteType.Whitespace
        }
        if (byte in PdfToken.ASCII_NUMBER) {
            return ByteType.NumLiteral
        }
        if (byte.toInt() == '%'.code) {
            return ByteType.AnnotationLiteral
        }
        if (byte.toInt() == '/'.code) {
            return ByteType.NameLiteral
        }
        if (byte in PdfToken.ASCII_ALPHABET) {
            return ByteType.Keyword
        }
        if (byte in PdfToken.ASCII_WRAPPER) {
            return ByteType.Wrapper
        }
        if (byte in PdfToken.ASCII_SPECIAL) {
            return ByteType.Operator
        }

        return ByteType.Unknown
    }

    fun isByteType(byte: Byte, byteType: ByteType): Boolean {
        when (byteType) {
            ByteType.NumLiteral -> {
                return byte in PdfToken.ASCII_NUMBER
            }
            ByteType.AnnotationLiteral -> {
                return byte != PdfToken.ASCII_LF
            }
            ByteType.NameLiteral -> {
                return byte.toInt() != 0
                        && byte.toInt() != '/'.code
                        && byte.toInt() != '%'.code
                        && byte !in PdfToken.ASCII_WRAPPER
                        && byte !in PdfToken.ASCII_WHITESPACE
            }
            ByteType.StrLiteral -> {
                return byte.toInt() != ')'.code
                        && byte in PdfToken.ASCII_ALL
            }
            ByteType.HexStrLiteral -> {
                return byte.toInt() != '>'.code
                        && byte in PdfToken.ASCII_STRING
            }
            ByteType.Keyword -> {
                return byte in PdfToken.ASCII_ALPHABET
            }
            ByteType.Wrapper -> {
                return byte.toInt() != '%'.code && byte in PdfToken.ASCII_SPECIAL
            }
            else -> return false
        }
    }

    enum class ByteType {
        Unknown,
        Whitespace,
        NumLiteral,
        StrLiteral,
        NameLiteral,
        HexStrLiteral,
        AnnotationLiteral,
        Keyword,
        Wrapper,
        Operator,
        Bytes
    }

}

class PdfToken(var kind: Kind, val content: ArrayList<Byte>?) {

    var isReal = false
    var isNegative = false

    companion object {
        /*
        ASCII_STRING : All characters able to string in ascii ( include SPACE )
        ASCII_ALPHABET : A~Z, a~z
        ASCII_NUMBER : '0' ~ '9'
        ASCII_WHITESPACE : NULL, HT, LF, FF, CR, SP
        ASCII_WRAPPER : ( ) < > [ ] { }
        ASCII_SPECIAL : All Special Charset (ex. !@#$%^&*()...)
         */
        val ASCII_LF: Byte = 10
        val ASCII_ALL = (0..127).toByteArray()
        val ASCII_HEXSTRING: Array<Byte> = (48..57).toByteArray() +
                (65..70).toByteArray() +
                (97..102).toByteArray()
        val ASCII_STRING: Array<Byte> = (32..126).toByteArray()
        val ASCII_ALPHABET: Array<Byte> = (65..90).toByteArray() + (97..122).toByteArray()
        val ASCII_NUMBER: Array<Byte> = (48..57).toByteArray()
        val ASCII_WHITESPACE = arrayOf<Byte>(0, 9, 10, 12, 13, 32)
        val ASCII_WRAPPER = arrayOf<Byte>(40, 41, 60, 62, 91, 93, 123, 125)
        val ASCII_SPECIAL: Array<Byte> = (33..46).toByteArray() +
                (58..64).toByteArray() +
                (91..96).toByteArray() +
                (123..126).toByteArray()
        val CHARSET_NAME: Array<Byte> = ASCII_ALPHABET + ASCII_NUMBER + arrayOf('/'.code.toByte())

        val STR_TO_KIND: Map<ByteArray, PdfToken.Kind> = mapOf(
            "True".toByteArray() to Kind.True,
            "False".toByteArray() to Kind.False,

            "obj".toByteArray() to Kind.StartObject,
            "endobj".toByteArray() to Kind.EndObject,
            "stream".toByteArray() to Kind.StartStream,
            "endstream".toByteArray() to Kind.EndStream,
            "R".toByteArray() to Kind.Indirect,

            "%".toByteArray() to Kind.Annotation,

            "(".toByteArray() to Kind.LeftParen,
            ")".toByteArray() to Kind.RightParen,
            "<".toByteArray() to Kind.LessThan,
            ">".toByteArray() to Kind.GreaterThan,
            "<<".toByteArray() to Kind.StartDict,
            ">>".toByteArray() to Kind.EndDict,

            "[".toByteArray() to Kind.LeftBracket,
            "]".toByteArray() to Kind.RightBracket

        )

    }


    enum class Kind {
        Unknown, EndOfToken, Keyword,

        Number, Name, String, HexString,
        Dict, Array,

        True,
        False,
        Null,

        Object,
        Stream,

        Annotation,
        Indirect,

        // Wrapper
        StartObject,
        EndObject,
        StartStream,
        EndStream,
        LessThan,
        GreaterThan,
        StartDict,
        EndDict,
        LeftParen,
        RightParen,
        LeftBracket,
        RightBracket
    }

}

private fun IntRange.toByteArray(): Array<Byte> {
    if (last < first) return arrayOf<Byte>()

    val result = Array<Byte>(last - first + 1) { i -> i.toByte() }
    var index = 0
    for (element in this) {
        result[index++] = element.toByte()
    }

    return result
}
