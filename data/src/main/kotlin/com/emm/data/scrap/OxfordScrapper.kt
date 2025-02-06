package com.emm.data.scrap

import com.emm.domain.Example
import com.emm.domain.Word
import com.emm.domain.WordContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class OxfordScrapper {

    private val connection: Connection by lazy { Jsoup.newSession() }

    suspend fun scrap(word: Word): WordContent = withContext(Dispatchers.IO) {
        val baseUrl = buildUrl(word.word)
        val doc: Document = connection.newRequest(baseUrl).get()
        val title: String = doc.select("h1.headword").text()
        val pos: String = doc.select("span.pos").text()
        val exampleList: List<Example> = extractExamples(doc)
        return@withContext WordContent(
            wordId = word.id,
            word = title,
            pos = pos,
            examples = exampleList,
        )
    }

    private fun extractExamples(doc: Document): List<Example> {
        val select: Elements = doc.select("ol.senses_multiple")
        return select.map(::buildExample)
    }

    private fun buildExample(element: Element): Example {
        val number: String = element.attr("sensenum")
        val title: String = element.select("span.cf").text()
        val titleSecond: String = element.select("span.def").text()
        val mutableMap = extractSentences(element)
        return Example(
            number = number,
            title = "$title $titleSecond",
            sentences = mutableMap.filter { it.isNotBlank() }
        )
    }

    private fun extractSentences(element: Element): MutableList<String> {
        val sentences = mutableListOf<String>()
        element.select("ul > li").forEach { li ->
            val second = li.select("span.x").text()
            val first = li.select("span.cf").text()
            sentences.add("$first $second")
        }
        return sentences
    }

    private fun buildUrl(word: String): String = BASE.plus(word)

    companion object {

        private const val BASE = "https://www.oxfordlearnersdictionaries.com/us/definition/english/"
    }
}