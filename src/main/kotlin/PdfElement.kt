package org.example

abstract class PdfElement() {

    val elements: ArrayList<PdfElement> = arrayListOf()

    abstract fun parse(pdfTokens: ArrayList<PdfToken>)
    abstract fun parse(pdfToken: PdfToken)

    open fun print() {
        for (element in elements) {
            element.print()
        }
    }
}

class PdfObject(var objNum: Int = -1, var genNum: Int = -1): PdfElement() {

    override fun print() {
        println("PdfObject $objNum $genNum")
        super.print()
    }

    override fun parse(pdfTokens: ArrayList<PdfToken>) {
        val pdfParser = PdfParser(pdfTokens)
        pdfParser.parse()
        elements.addAll(pdfParser.pdfElements)
    }

    override fun parse(pdfToken: PdfToken) {

    }


}
class PdfIndirect(var objNum: Int = -1, var genNum: Int = -1): PdfElement() {

    override fun print() {
        println("PdfIndirect $objNum $genNum")
    }

    override fun parse(pdfTokens: ArrayList<PdfToken>) {

    }

    override fun parse(pdfToken: PdfToken) {

    }


}

class PdfInteger(): PdfElement() {

    var value: Int? = null

    override fun print() {
        print("PdfInteger : $value || ")
    }
    override fun parse(pdfTokens: ArrayList<PdfToken>) {
    }

    override fun parse(pdfToken: PdfToken) {

        if (pdfToken.content == null) {
            throw Exception("PdfNumber : No Content in Token\n" +
                    "Kind : ${pdfToken.kind}\n" +
                    "Content : ${pdfToken.content}\n" +
                    "Real : ${pdfToken.isReal}\n" +
                    "Negative : ${pdfToken.isNegative}")
        }

        value = atoi(pdfToken.content.toByteArray())

        value = if (pdfToken.isNegative) -value!! else value

    }

    fun atoi(byteArray: ByteArray): Int {

        var result = 0
        for (byte in byteArray) {
            val k = byte - '0'.code
            result += 10 * result + k
        }

        return result
    }

}


class PdfReal(): PdfElement() {

    var value: Double? = null

    override fun print() {
        print("PdfReal : $value || ")
    }
    override fun parse(pdfTokens: ArrayList<PdfToken>) {

    }

    override fun parse(pdfToken: PdfToken) {

        if (pdfToken.content == null) {
            throw Exception("PdfNumber : No Content in Token\n" +
                    "Kind : ${pdfToken.kind}\n" +
                    "Content : ${pdfToken.content}\n" +
                    "Real : ${pdfToken.isReal}\n" +
                    "Negative : ${pdfToken.isNegative}")
        }

        value = atof(pdfToken.content.toByteArray())
        value = if (pdfToken.isNegative) -value!! else value

    }

    private fun atof(byteArray: ByteArray): Double {
        var integer: Double = 0.0
        var fraction: Double = 0.0
        var exp = 0
        var i = 0
        do {
            val byte = byteArray[i]
            if (byte.toInt() == '.'.code) break
            val k = byte - '0'.code
            integer = 10 * integer + k
            i++
        } while (i < byteArray.size)
        i++
        while (i < byteArray.size) {
            val byte = byteArray[i]
            val k = byte - '0'.code
            fraction = 10 * fraction + k
            exp++
            i++
        }
        while (exp > 0) {
            fraction *= 0.1
            exp--
        }

        return integer + fraction
    }

}

