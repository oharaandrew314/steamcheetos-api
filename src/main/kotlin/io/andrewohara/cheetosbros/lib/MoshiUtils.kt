package io.andrewohara.cheetosbros.lib

import com.squareup.moshi.*
import java.time.Instant

class IsoInstantJsonAdapter: JsonAdapter<Instant>() {

    @FromJson
    override fun fromJson(reader: JsonReader): Instant {
        val string = reader.nextString()
        return Instant.parse(string)
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: Instant?) {
        val serialized: String? = value?.toString()

        writer.value(serialized)
    }
}