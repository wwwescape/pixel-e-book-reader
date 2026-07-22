package com.wwwescape.pixelebookreader.data.parser

import com.wwwescape.pixelebookreader.data.parser.epub.EpubCoverParser
import com.wwwescape.pixelebookreader.data.parser.epub.EpubFileParser
import com.wwwescape.pixelebookreader.data.parser.epub.EpubTextParser
import com.wwwescape.pixelebookreader.data.parser.fb2.Fb2CoverParser
import com.wwwescape.pixelebookreader.data.parser.fb2.Fb2FileParser
import com.wwwescape.pixelebookreader.data.parser.fb2.Fb2TextParser
import com.wwwescape.pixelebookreader.data.parser.html.HtmlCoverParser
import com.wwwescape.pixelebookreader.data.parser.html.HtmlFileParser
import com.wwwescape.pixelebookreader.data.parser.html.HtmlTextParser
import com.wwwescape.pixelebookreader.data.parser.md.MarkdownCoverParser
import com.wwwescape.pixelebookreader.data.parser.md.MarkdownFileParser
import com.wwwescape.pixelebookreader.data.parser.md.MarkdownTextParser
import com.wwwescape.pixelebookreader.data.parser.pdf.PdfCoverParser
import com.wwwescape.pixelebookreader.data.parser.pdf.PdfFileParser
import com.wwwescape.pixelebookreader.data.parser.pdf.PdfTextParser
import com.wwwescape.pixelebookreader.data.parser.txt.TxtCoverParser
import com.wwwescape.pixelebookreader.data.parser.txt.TxtFileParser
import com.wwwescape.pixelebookreader.data.parser.txt.TxtTextParser

private data class ParserSet(
    val fileParser: FileParser,
    val coverParser: CoverParser,
    val textParser: TextParser,
)

/** Resolves the right [FileParser]/[CoverParser]/[TextParser] trio for a book file by
 * extension — the single place Browse's import flow and the reader need to
 * know about to support a new format. */
object BookParsers {

    private val byExtension: Map<String, ParserSet> = mapOf(
        "txt" to ParserSet(TxtFileParser(), TxtCoverParser(), TxtTextParser()),
        "md" to ParserSet(MarkdownFileParser(), MarkdownCoverParser(), MarkdownTextParser()),
        "html" to ParserSet(HtmlFileParser(), HtmlCoverParser(), HtmlTextParser()),
        "htm" to ParserSet(HtmlFileParser(), HtmlCoverParser(), HtmlTextParser()),
        "fb2" to ParserSet(Fb2FileParser(), Fb2CoverParser(), Fb2TextParser()),
        "epub" to ParserSet(EpubFileParser(), EpubCoverParser(), EpubTextParser()),
        "pdf" to ParserSet(PdfFileParser(), PdfCoverParser(), PdfTextParser()),
    )

    fun isSupported(fileName: String): Boolean = extensionOf(fileName) in byExtension

    /** True for formats with genuine fixed pages — currently just PDF — which get the paged
     * reader ([com.wwwescape.pixelebookreader.ui.screens.reader.PdfPageReaderScreen]) instead of
     * the continuous-scroll one every other format uses. */
    fun isPagedFormat(fileName: String): Boolean = extensionOf(fileName) == "pdf"

    fun fileParserFor(fileName: String): FileParser? = byExtension[extensionOf(fileName)]?.fileParser

    fun coverParserFor(fileName: String): CoverParser? = byExtension[extensionOf(fileName)]?.coverParser

    fun textParserFor(fileName: String): TextParser? = byExtension[extensionOf(fileName)]?.textParser

    private fun extensionOf(fileName: String): String = fileName.substringAfterLast('.', "").lowercase()
}
