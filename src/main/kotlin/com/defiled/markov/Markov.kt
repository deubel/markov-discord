package com.defiled.markov

import java.util.*

internal class Markov {

    private val chain: Hashtable<String, Vector<String>> = Hashtable()

    fun addPhrase(text: String) {
        println("Adding phrase: $text")
        val words = text.split(' ')

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
                words.size - 1 -> {
                    val ends = chain.getOrPut("_end") { Vector() }
                    ends += word
                }
                else -> {
                    if (index != words.lastIndex) {
                        val suffix = chain.getOrPut(word) { Vector() }
                        suffix += words[index + 1]
                    }
                }
            }
        }
    }

    fun generate(): String {
        val phrase = Vector<String>()
        var word = chain["_start"]!!.random()
        phrase += word

        while (word.isNotBlank() && word.last() != '.') {
            word = chain[word]!!.random()
            phrase += word
        }

        return phrase.joinToString(" ").also { println("Generated Markov chain: $it") }
    }

}