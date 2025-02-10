package com.emm.data.wordcontent

import com.emm.domain.Example
import com.emm.domain.SourceType
import com.emm.domain.Word
import com.emm.domain.WordContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.UUID

class OxfordScrapper {

    private val connection: Connection by lazy { Jsoup.newSession() }

    suspend fun scrap(word: Word): WordContent = withContext(Dispatchers.IO) {
        val baseUrl = buildUrl(word.word)
        val doc: Document = connection.newRequest(baseUrl).get()
        val title: String = doc.select("h1.headword").text().trim()
        val pos: String = doc.select("span.pos").text().trim()
        val exampleList: List<Example> = extractExamples(doc)
        return@withContext WordContent(
            wordContentId = UUID.randomUUID().toString(),
            word = title,
            pos = pos,
            sourceType = SourceType.SCRAPPING,
            examples = exampleList,
        )
    }

    private fun extractExamples(doc: Document): List<Example> {
        val select: Elements = doc.select("ol.senses_multiple")
            .first()
            ?.select("li.sense") ?: Elements()
        return select.map(::buildExample)
    }

    private fun buildExample(element: Element): Example {
        val number: String = element.attr("sensenum")
        val title: String = element.select("span.cf").text().trim()
        val titleSecond: String = element.select("span.def").text().trim()
        val children: Elements = element.select("ul.examples").first()?.children() ?: Elements()
        val mutableMap = extractSentences(children)
        val sentences = mutableMap.filter { it.isNotBlank() }
        return Example(
            number = number,
            title = "$title $titleSecond",
            sentences = sentences
        )
    }

    private fun extractSentences(elements: Elements): List<String> {
        val sentences = mutableListOf<String>()
        elements.forEach { li ->
            val second = li.select("span.x").text().trim()
            val first = li.select("span.cf").text().trim()
            sentences.add("$first $second".trim())
        }
        return sentences.filter(String::isNotBlank)
    }

    private fun buildUrl(word: String): String = BASE.plus(word)

    companion object {

        private const val BASE = "https://www.oxfordlearnersdictionaries.com/us/definition/english/"
    }
}