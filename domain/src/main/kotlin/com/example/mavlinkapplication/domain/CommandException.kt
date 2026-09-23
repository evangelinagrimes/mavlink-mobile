package com.example.mavlinkapplication.domain

/**
 * Failure reasons for a MAVLink command (MAV_CMD via COMMAND_LONG), distinguishing
 * "the vehicle actively refused this" from "we never heard back" — the UI shows these
 * differently since one means retrying won't help without a different action first.
 */
sealed class CommandException(message: String) : Exception(message) {
    /** The vehicle sent COMMAND_ACK with a non-zero MAV_RESULT — it refused the command. */
    data class Rejected(val mavResult: Int) : CommandException("Command rejected: MAV_RESULT=$mavResult")

    /** No COMMAND_ACK arrived after retries — could be a dropped link or an unsupported command. */
    data object NoAck : CommandException("No acknowledgement received after retries")

    data object NotConnected : CommandException("Not connected")
}
