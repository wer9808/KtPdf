package org.example

class PdfParser(val tokens: ArrayList<PdfToken>) {

    val pdfElements: ArrayList<PdfElement> = arrayListOf()
    var current = 0

    fun printElements() {
        for (element in pdfElements) {
            element.print()
        }
    }
    fun parse() {

        if (tokens.size == 0) {
            println("Parsing Error : No Tokens")
            return
        }

        while (current < tokens.size) {
            val token = tokens[current]
            when (token.kind) {
                PdfToken.Kind.EndOfToken -> {
                    return
                }
                PdfToken.Kind.StartObject -> {
                    parseObject()
                }
                PdfToken.Kind.Number -> {
                    parseNumber()
                }
                PdfToken.Kind.Indirect -> {
                    parseIndirect()
                }
                else -> {
                    current++
                }
            }
        }
    }

    private fun parseIndirect() {
        val size = pdfElements.size
        val objNum = (pdfElements[size - 2] as PdfInteger).value
        val genNum = (pdfElements[size - 1] as PdfInteger).value


        if (objNum == null || genNum == null) {
            throw Error("Parsing Error : Invalid Object Number\n"
                    + "$current : $objNum $genNum")
        }

        val pdfIndirect = PdfIndirect(objNum, genNum)
        current++

        pdfElements.add(pdfIndirect)
    }

    fun parseObject() {

        val size = pdfElements.size
        val objNum = (pdfElements[size - 2] as PdfInteger).value
        val genNum = (pdfElements[size - 1] as PdfInteger).value

        if (objNum == null || genNum == null) {
            throw Error("Parsing Error : Invalid Object Number\n"
                    + "$current : $objNum $genNum")
        }

        val pdfObject = PdfObject(objNum, genNum)
        val childTokens: ArrayList<PdfToken> = arrayListOf()
        while (true) {
            val token = tokens[++current]
            if (token.kind == PdfToken.Kind.EndObject) {
                break
            }
            childTokens.add(token)
        }

        pdfObject.parse(childTokens)

        pdfElements.add(pdfObject)
    }

    fun parseNumber() {
        val token = tokens[current]
        val pdfNumber = if (token.isReal) PdfReal() else PdfInteger()
        pdfNumber.parse(token)
        pdfElements.add(pdfNumber)
        current++
    }

}
