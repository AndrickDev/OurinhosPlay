package com.ourinhosplay.app.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class Profile(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    val tag: Int? = null,
    val role: String = "user",
) {
    /** "Nome#0042", ou só o nome se a tag ainda não existir. */
    val fullName: String
        get() = displayName.orEmpty() + (tag?.let { "#" + it.toString().padStart(4, '0') } ?: "")
}

object ProfileRepository {
    private val db get() = Supabase.client

    suspend fun current(): Profile? {
        val uid = db.auth.currentUserOrNull()?.id ?: return null
        return db.from("profiles").select { filter { eq("id", uid) } }.decodeSingleOrNull<Profile>()
    }

    /** Define o nome uma vez; o servidor sorteia a tag #0000-#9999. */
    suspend fun setDisplayName(name: String) {
        db.postgrest.rpc("set_display_name", buildJsonObject { put("p_name", name) })
    }
}
