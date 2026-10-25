package com.disheveled.bills.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * What an expense was for. Declaration order is the display order everywhere, with
 * [Lainnya] always last.
 *
 * [key] is what gets stored and what the model is asked to return, so it must never change
 * once data exists; the constant names are free to be refactored. [label] is what people
 * read, in the app and in the facts the monthly AI note is written from.
 */
@Serializable(with = CategorySerializer::class)
enum class Category(val key: String, val label: String) {
    Makan("makan", "Makan"),
    Kopi("kopi", "Kopi & Jajan"),
    Belanja("belanja", "Belanja"),
    Transport("transport", "Transport"),
    Tagihan("tagihan", "Tagihan"),
    Hiburan("hiburan", "Hiburan"),
    Kesehatan("kesehatan", "Kesehatan"),
    Lainnya("lainnya", "Lainnya");

    companion object {
        fun fromKey(key: String): Category? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Writes [Category.key] instead of the constant name, which is what kotlinx.serialization
 * would do by default and what would make a rename break every saved split. Public only
 * because serializers can't be less visible than the type they serve.
 */
object CategorySerializer : KSerializer<Category> {
    override val descriptor = PrimitiveSerialDescriptor("com.disheveled.bills.model.Category", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Category) = encoder.encodeString(value.key)

    // A key this build doesn't know can only come from a newer one; Lainnya keeps the entry.
    override fun deserialize(decoder: Decoder): Category =
        Category.fromKey(decoder.decodeString()) ?: Category.Lainnya
}
