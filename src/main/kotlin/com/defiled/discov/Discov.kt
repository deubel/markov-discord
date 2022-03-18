package com.defiled.discov

import com.defiled.markov.Markov
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

internal object Discov {

    lateinit var token: String
    lateinit var storageFile: File
    lateinit var client: DiscordClient
    lateinit var gateway: GatewayDiscordClient

    private val gson = GsonBuilder().registerTypeAdapter(Snowflake::class.java, SnowflakeTypeAdapter).enableComplexMapKeySerialization().create()

    private lateinit var markovs: MutableMap<Snowflake, Markov>

    private var lastSave = 0L

    fun initialize(gateway: GatewayDiscordClient) {
        markovs = if (storageFile.exists()) {
            val json = ungzip(storageFile.readBytes())
            gson.fromJson(json, TypeToken.getParameterized(MutableMap::class.java, Snowflake::class.java, Markov::class.java).type)
        } else {
            mutableMapOf()
        }

        gateway.on(MessageCreateEvent::class.java).subscribe { event ->
            try {
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

                        if (System.currentTimeMillis() - lastSave > TimeUnit.MINUTES.toMillis(15)) {
                            val json = gson.toJson(markovs)
                            storageFile.writeBytes(gzip(json))
                            lastSave = System.currentTimeMillis()
                        }
                    }
                } else if (author.id != gateway.selfId) {
                    if (msg.content.startsWith("!markov purge")) {
                        markovs.remove(author.id)
                    }
                    val userId = msg.userMentions.firstOrNull()?.id ?: gateway.selfId ?: return@subscribe
                    val markov = markovs[userId] ?: return@subscribe
                    val generated = markov.generate() ?: return@subscribe
                    channel.createMessage(generated).block()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        gateway.onDisconnect().block()
    }

    private fun formatContent(content: String): String {
        val capitalized = content.capitalize()
        return if (capitalized.lastOrNull() in setOf('.', '?', '!')) capitalized else "$capitalized."
    }

    private fun gzip(content: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter().use { it.write(content) }
        return bos.toByteArray()
    }

    private fun ungzip(content: ByteArray) = GZIPInputStream(content.inputStream()).bufferedReader().use { it.readText() }

}

fun main(args: Array<String>) {
    Discov.token = args[0]
    Discov.storageFile = File(args[1])
    Discov.client = DiscordClient.create(Discov.token)
    Discov.gateway = Discov.client.login().block()!!

    Discov.initialize(Discov.gateway)
}
