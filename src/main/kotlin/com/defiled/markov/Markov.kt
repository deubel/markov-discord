package com.defiled.markov

import java.util.*

internal class Markov {

    private val chain: Hashtable<String, Vector<String>> = Hashtable()

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
                    val starts = chain.getOrPut("_start") { Vector() }
                    starts += word

                    if (index != words.lastIndex) {
                        val suffix = chain.getOrPut(word) { Vector() }
                        suffix += words[index + 1]
                    }
                }
                words.lastIndex -> {
                    val ends = chain.getOrPut("_end") { Vector() }
                    ends += word
                }
                else -> {
                    val suffix = chain.getOrPut(word) { Vector() }
                    suffix += words[index + 1]
                }
            }
        }
    }

    fun generate(): String? {
        val phrase = Vector<String>()
        var word = (chain["_start"] ?: return null).random()
        phrase += word

        while (word.isNotBlank() && word.last() !in setOf('.', '?')) {
            word = (chain[word] ?: return null).random()
            phrase += word
        }

        return phrase.joinToString(" ").also { println("Generated Markov chain: $it") }
    }

}