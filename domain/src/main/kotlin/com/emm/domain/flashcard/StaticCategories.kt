package com.emm.domain.flashcard

data class StaticCategories(
    val id: Int,
    val name: String,
)

val staticCategories = listOf(
    StaticCategories(1, "Basic Vocabulary"),
    StaticCategories(2, "Greetings and Introductions"),
    StaticCategories(3, "Alphabet and Pronunciation"),
    StaticCategories(4, "Numbers and Dates"),
    StaticCategories(5, "Colors and Objects"),
    StaticCategories(6, "Verb To Be"),
    StaticCategories(7, "Articles and Nouns"),
    StaticCategories(8, "Personal Pronouns"),
    StaticCategories(9, "Adjectives and Comparisons"),
    StaticCategories(10, "Simple Present"),
    StaticCategories(11, "Present Continuous"),
    StaticCategories(12, "Simple Past"),
    StaticCategories(13, "Past Continuous"),
    StaticCategories(14, "Future Tense (will, going to)"),
    StaticCategories(15, "Modal Verbs"),
    StaticCategories(16, "Questions and Answers"),
    StaticCategories(17, "Prepositions"),
    StaticCategories(18, "Connectors and Common Phrases"),
    StaticCategories(19, "Perfect Tenses"),
    StaticCategories(20, "Conditionals"),
    StaticCategories(21, "Phrasal Verbs"),
    StaticCategories(22, "Conversational English"),
    StaticCategories(23, "English for Travel"),
    StaticCategories(24, "Business English"),
    StaticCategories(25, "Common Mistakes"),
    StaticCategories(26, "Idiomatic Expressions"),
    StaticCategories(27, "Listening and Audio Comprehension"),
    StaticCategories(28, "Reading Comprehension"),
    StaticCategories(29, "Writing Skills"),
    StaticCategories(30, "Speaking and Pronunciation")
)

val difficult = listOf("basic", "intermediate", "advanced")
