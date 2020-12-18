package com.defiled.markov

internal class Markov(private val chain: MutableMap<String, MutableList<String>> = mutableMapOf()) {

    fun addPhrase(text: String, minWords: Int = 4) {
        println("Adding phrase: $text")
        val words = text.split(' ')
        if (words.size < minWords) {
            println("Phrase has less words than the minimum of $minWords")
            return
        }

        words.forEachIndexed { index, word ->
            when (index) {
                0 -> {
                    val starts = chain.getOrPut("_start") { mutableListOf() }
                    starts += word

                    if (index != words.lastIndex) {
                        val suffix = chain.getOrPut(word) { mutableListOf() }
                        suffix += words[index + 1]
                    }
                }
                words.lastIndex -> {
                    val ends = chain.getOrPut("_end") { mutableListOf() }
                    ends += word
                }
                else -> {
                    val suffix = chain.getOrPut(word) { mutableListOf() }
                    suffix += words[index + 1]
                }
            }
        }
    }

    fun generate(): String? {
        val phrase = mutableListOf<String>()
        var word = (chain["_start"] ?: return null).random()
        phrase += word

        while (word.isNotBlank() && word.last() !in setOf('.', '?', '!')) {
            word = (chain[word] ?: return null).random()
            phrase += word
        }

        return phrase.joinToString(" ").also { println("Generated Markov chain: $it") }
    }

}