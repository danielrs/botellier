package org.botellier.serializer

import org.botellier.store.*

class JsonSerializer(override val value: StoreType,
                  var pretty: Boolean = true,
                  var indent: String = "    ") : Serializer {

    override fun serialize(): ByteArray = print().toByteArray()

    fun print(): String {
        val builder = StringBuilder()
        render(builder, "", value)
        return builder.toString()
    }

    private fun render(builder: StringBuilder, indent: String, value: StoreType) {
        when (value) {
            is IntValue -> builder.append(value.unwrap())
            is FloatValue -> builder.append(value.unwrap())
            is StringValue -> builder.append("\"$value\"")
            is NilValue -> {} // skips nil
            is ListValue -> renderList(builder, indent, value)
            is SetValue -> renderList(builder, indent, value.map(String::toValue))
            is MapValue -> renderMap(builder, indent, value)
        }
    }

    private fun renderList(builder: StringBuilder, indent: String, list: Iterable<StorePrimitive>) {
        builder.append('[')
        val it = list.iterator()
        while (it.hasNext()) {
            val v = it.next()
            render(builder, indent, v)
            if (it.hasNext()) {
                builder.append(",")
            }
        }
        builder.append(']')
    }

    private fun renderMap(builder: StringBuilder, indent: String, map: Iterable<Map.Entry<String, StoreValue>>) {
        val newline = if (pretty) "\n" else ""
        val braceOpen = "{$newline"
        val braceClose = if (pretty) "$newline$indent}" else "}"

        builder.append(braceOpen)
        val it = map.iterator()
        while (it.hasNext()) {
            val v = it.next()
            renderKeyValue(builder, indent + this.indent, v)
            if (it.hasNext()) {
                builder.append(if (pretty) ",\n" else ",")
            }
        }
        builder.append(braceClose)
    }

    private fun renderKeyValue(builder: StringBuilder, indent: String, keyValue: Map.Entry<String, StoreValue>) {
        builder.append("${if (pretty) indent else ""}\"${keyValue.key}\": ")
        render(builder, indent, keyValue.value)
    }
}

// Extensions.
fun StoreType.toJson(pretty: Boolean = true, indent: String = "    "): String =
        JsonSerializer(this, pretty, indent).print()
