package com.fandom.auchatstudio.core

enum class MessageType {
    /** A normal chat bubble. */
    TEXT,

    /** A mock voice note drawn as a waveform. */
    VOICE,

    /** A picture copied into internal storage. */
    IMAGE,

    /** A centred narrator line ("2 a.m.", "Minho has seen it"). */
    NARRATION;

    companion object {
        fun fromName(name: String?): MessageType = entries.firstOrNull { it.name == name } ?: TEXT
    }
}
