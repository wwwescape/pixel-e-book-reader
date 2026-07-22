package com.wwwescape.pixelebookreader.data.parser.epub

import android.content.Context
import android.net.Uri
import com.wwwescape.pixelebookreader.data.parser.copyToCacheFile
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.File
import java.net.URLDecoder
import java.util.zip.ZipFile

/** EPUB is a ZIP archive, and locating an arbitrary entry (the OPF, the NCX, each spine content
 * doc) needs random access into it — a plain SAF [android.content.ContentResolver] stream only
 * supports sequential reads, so the whole file is copied to [Context.getCacheDir] first and
 * opened as a real [ZipFile]. Callers must [close] the result when done. */
internal class EpubDocument private constructor(
    private val tempFile: File,
    private val zip: ZipFile,
    val opf: Document,
    val opfBaseDir: String,
) : AutoCloseable {

    private val manifest: Map<String, Pair<String, String>> = opf.select("manifest > item").associate { item ->
        item.attr("id") to (item.attr("href") to item.attr("media-type"))
    }

    val spineHrefs: List<String> = opf.select("spine > itemref").mapNotNull { manifest[it.attr("idref")]?.first }

    fun readEntry(hrefRelativeToOpf: String): ByteArray? {
        val path = resolvePath(hrefRelativeToOpf)
        val entry = zip.getEntry(path) ?: zip.getEntry(path.removePrefix("/")) ?: return null
        return zip.getInputStream(entry).use { it.readBytes() }
    }

    /** The NCX (EPUB2 table of contents) document, if the manifest references one. */
    fun readNcx(): Document? {
        val tocId = opf.select("spine").firstOrNull()?.attr("toc")
        val ncxHref = tocId?.let { manifest[it]?.first }
            ?: manifest.values.firstOrNull { it.second.contains("ncx") }?.first
            ?: return null
        val bytes = readEntry(ncxHref) ?: return null
        return Jsoup.parse(bytes.inputStream(), null, "", Parser.xmlParser())
    }

    /** Resolves the manifest item referenced by `<meta name="cover" content="…">`, or falls
     * back to a manifest image item whose id/href suggests it's the cover. */
    fun findCoverEntryHref(): String? {
        val coverId = opf.select("metadata > meta[name=cover]").firstOrNull()?.attr("content")
        manifest[coverId]?.first?.let { return it }
        return manifest.values.firstOrNull { (href, mediaType) ->
            mediaType.startsWith("image/") && (href.contains("cover", ignoreCase = true))
        }?.first
    }

    private fun resolvePath(href: String): String {
        val decoded = runCatching { URLDecoder.decode(href, "UTF-8") }.getOrDefault(href)
        return if (opfBaseDir.isEmpty()) decoded else "$opfBaseDir/$decoded"
    }

    override fun close() {
        runCatching { zip.close() }
        tempFile.delete()
    }

    companion object {
        fun open(context: Context, uri: Uri): EpubDocument? {
            val tempFile = copyToCacheFile(context, uri, "epub_", ".epub") ?: return null

            val zip = runCatching { ZipFile(tempFile) }.getOrNull() ?: run {
                tempFile.delete()
                return null
            }

            val containerEntry = zip.getEntry("META-INF/container.xml") ?: run {
                zip.close()
                tempFile.delete()
                return null
            }
            val container = zip.getInputStream(containerEntry).use {
                Jsoup.parse(it, null, "", Parser.xmlParser())
            }
            val opfPath = container.select("rootfile").firstOrNull()?.attr("full-path") ?: run {
                zip.close()
                tempFile.delete()
                return null
            }
            val opfEntry = zip.getEntry(opfPath) ?: run {
                zip.close()
                tempFile.delete()
                return null
            }
            val opf = zip.getInputStream(opfEntry).use {
                Jsoup.parse(it, null, "", Parser.xmlParser())
            }
            val opfBaseDir = opfPath.substringBeforeLast('/', "")

            return EpubDocument(tempFile, zip, opf, opfBaseDir)
        }
    }
}

/** Matches an element's local tag name, ignoring any XML namespace prefix (e.g. `dc:title` and
 * plain `title` both match `"title"`) — Jsoup's XML parser doesn't resolve namespaces, so OPF/
 * NCX documents that do or don't prefix their tags both need to work. */
internal fun Element.firstByLocalName(vararg localNames: String): Element? =
    children().firstOrNull { child -> localNames.any { child.tagName().substringAfter(':').equals(it, ignoreCase = true) } }

/** Same namespace-agnostic match as [firstByLocalName], but returns every match — needed for
 * repeatable OPF metadata elements like `dc:subject` (a book can have several). */
internal fun Element.allByLocalName(vararg localNames: String): List<Element> =
    children().filter { child -> localNames.any { child.tagName().substringAfter(':').equals(it, ignoreCase = true) } }
