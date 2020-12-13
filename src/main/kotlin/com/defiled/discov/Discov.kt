package com.defiled.discov

import com.defiled.markov.Markov
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent

internal object Discov {

    lateinit var token: String
    lateinit var client: DiscordClient
    lateinit var gateway: GatewayDiscordClient

    private val markovs = mutableMapOf<Snowflake, Markov>()

    fun addListeners(gateway: GatewayDiscordClient) {
        gateway.on(MessageCreateEvent::class.java).subscribe { event ->
            val msg = event.message
            val channel = msg.channel.block() ?: return@subscribe

            val author = msg.authorAsMember.block() ?: return@subscribe
            val authorMarkov = markovs.getOrPut(author.id) { Markov() }
            authorMarkov.addPhrase(formatContent(msg.content))

//            val mention = msg.userMentions.single().block() ?: return@subscribe
//            val mentionedMarkov = markovs.getOrPut(mention.id) { fetchMarkov(channel, msg, mention.id) }

            if (msg.content.startsWith("!markov") && author.id != gateway.selfId) {
                val mention = msg.userMentions.singleOrEmpty().block() ?: return@subscribe
                val mentionedMarkov = markovs[mention.id] ?: return@subscribe
                channel.createMessage(mentionedMarkov.generate()).block()
            }
        }

        gateway.onDisconnect().block()
    }

    fun fetchMarkov(channel: MessageChannel, message: Message, userId: Snowflake) = Markov().apply {
        val messages = channel.getMessagesBefore(message.id).collectList().block() ?: return@apply
        messages
            .filter {
                it.authorAsMember.block()?.id == userId
            }
            .map { formatContent(it.content) }
            .forEach(this::addPhrase)
        addPhrase(message.content)
    }

    fun formatContent(content: String): String {
        val capitalized = content.capitalize()
        return if (capitalized.endsWith(".")) capitalized else "$capitalized."
    }

}

fun main(args: Array<String>) {
    Discov.token = args[0]
    Discov.client = DiscordClient.create(Discov.token)
    Discov.gateway = Discov.client.login().block()!!

    Discov.addListeners(Discov.gateway)
}