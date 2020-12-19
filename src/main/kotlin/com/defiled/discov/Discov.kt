package com.defiled.discov

import com.defiled.markov.Markov
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import java.io.File

internal object Discov {

    lateinit var token: String
    lateinit var storageFile: File
    lateinit var client: DiscordClient
    lateinit var gateway: GatewayDiscordClient

    private val gson = GsonBuilder().setPrettyPrinting().registerTypeAdapter(Snowflake::class.java, SnowflakeTypeAdapter).enableComplexMapKeySerialization().create()

    private lateinit var markovs: MutableMap<Snowflake, Markov>

    fun initialize(gateway: GatewayDiscordClient) {
        markovs = if (storageFile.exists()) gson.fromJson(storageFile.readText(), TypeToken.getParameterized(MutableMap::class.java, Snowflake::class.java, Markov::class.java).type) else mutableMapOf()

        gateway.on(MessageCreateEvent::class.java).subscribe { event ->
            val msg = event.message
            val channel = msg.channel.block() ?: return@subscribe

            val author = msg.authorAsMember.block() ?: return@subscribe

            if (!msg.content.startsWith("!markov")) {
                if (author.id != gateway.selfId) {
                    val authorMarkov = markovs.getOrPut(author.id) { Markov() }
                    val globalMarkov = markovs.getOrPut(gateway.selfId) { Markov() }
                    val formatted = formatContent(msg.content)
                    authorMarkov.addPhrase(formatted)
                    globalMarkov.addPhrase(formatted)
                }
            } else if (author.id != gateway.selfId) {
                if (msg.content.startsWith("!markov purge")) {
                    markovs.remove(author.id)
                }
                val mentionId = msg.userMentions.blockFirst()?.id ?: gateway.selfId ?: return@subscribe
                val mentionedMarkov = markovs[mentionId] ?: return@subscribe
                val generated = mentionedMarkov.generate() ?: return@subscribe
                channel.createMessage(generated).block()
            }

            storageFile.writeText(gson.toJson(markovs))
        }

        gateway.onDisconnect().block()
    }

    private fun formatContent(content: String): String {
        val capitalized = content.capitalize()
        return if (capitalized.lastOrNull() in setOf('.', '?', '!')) capitalized else "$capitalized."
    }

}

fun main(args: Array<String>) {
    Discov.token = args[0]
    Discov.storageFile = File(args[1])
    Discov.client = DiscordClient.create(Discov.token)
    Discov.gateway = Discov.client.login().block()!!

    Discov.initialize(Discov.gateway)
}