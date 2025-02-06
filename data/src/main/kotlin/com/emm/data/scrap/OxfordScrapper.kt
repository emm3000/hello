package com.emm.data.scrap

import com.emm.domain.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class OxfordScrapper {

    private val connection: Connection by lazy { Jsoup.newSession() }

    suspend fun scrap(word: Word): WordContentHolder = withContext(Dispatchers.IO) {
        val baseUrl = buildUrl(word.word)
        val doc: Document = connection.newRequest(baseUrl).get()
        val title: String = doc.select("h1.headword").text().trim()
        val pos: String = doc.select("span.pos").text().trim()
        val exampleList: List<ExampleHolder> = extractExamples(doc)
        return@withContext WordContentHolder(
            wordId = word.id,
            wordFromScrap = title,
            pos = pos,
            examples = exampleList,
        )
    }

    private fun extractExamples(doc: Document): List<ExampleHolder> {
        val select: Elements = doc.select("ol.senses_multiple")
            .first()
            ?.select("li.sense") ?: Elements()
        return select.map(::buildExample)
    }

    private fun buildExample(element: Element): ExampleHolder {
        val number: String = element.attr("sensenum")
        val title: String = element.select("span.cf").text().trim()
        val titleSecond: String = element.select("span.def").text().trim()
        val children: Elements = element.select("ul.examples").first()?.children() ?: Elements()
        val mutableMap = extractSentences(children)
        val sentences = mutableMap.filter { it.isNotBlank() }
        return ExampleHolder(
            number = number,
            title = "$title $titleSecond",
            sentences = sentences
        )
    }

    private fun extractSentences(elements: Elements): MutableList<String> {
        val sentences = mutableListOf<String>()
        elements.forEach { li ->
            val second = li.select("span.x").text().trim()
            val first = li.select("span.cf").text().trim()
            sentences.add("$first $second".trim())
        }
        return sentences
    }

    private fun buildUrl(word: String): String = BASE.plus(word)

    companion object {

        private const val BASE = "https://www.oxfordlearnersdictionaries.com/us/definition/english/"
    }
}