package com.ourinhosplay.app.data

/** Inicial maiúscula do nome para o "rosto" no cartão; "?" se não houver nome. */
fun initialOf(name: String?): String =
    name?.trim()?.firstOrNull()?.uppercase() ?: "?"

/** Quantos participantes ficam fora dos rostos mostrados (para o "+N"). */
fun hiddenFaces(taken: Int, shown: Int): Int = (taken - shown).coerceAtLeast(0)
