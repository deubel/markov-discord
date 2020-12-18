package com.defiled.discov

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import discord4j.common.util.Snowflake

internal object SnowflakeTypeAdapter : TypeAdapter<Snowflake>() {

    override fun write(output: JsonWriter, value: Snowflake?) {
        if (value == null) output.nullValue() else {
            output.beginObject()
            output.name("snowflake").value(value.asLong())
            output.endObject()
        }
    }

    override fun read(input: JsonReader) = if (input.peek() != JsonToken.BEGIN_OBJECT) null else {
        input.beginObject()
        (if (input.nextName() == "snowflake") Snowflake.of(input.nextLong()) else null).also { input.endObject() }
    }

}