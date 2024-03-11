package org.example

import java.io.File
import kotlin.time.measureTime

fun main() {
    println("Hello World!")

    val rootDir = System.getProperty("user.dir")
    val pdfPath = rootDir + "/src/main/resources/pdf"

    val pdfFile = File(pdfPath, "pdf-example.pdf")

    val pdfLexer = PdfLexer(pdfFile)
    PdfLexer.debug = true

    val time = measureTime {
        pdfLexer.scan()
    }
    //pdfLexer.printTokens(1)
    val pdfParser = PdfParser(pdfLexer.pdfTokens)
    pdfParser.parse()
    pdfParser.printElements()
    println(time)

}