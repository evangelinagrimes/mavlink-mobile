package com.example.mavlinkapplication.domain

/**
 * ArduPilot Rover drive modes, keyed by their raw custom_mode value.
 *
 * Mode numbers sourced from APMrover2/mode.h in the ArduPilot repository.
 * ASSUMED CORRECT — verify against current ArduPilot source if a mode looks wrong.
 * Note: MAVSDK's built-in mode enum is PX4-oriented and does not map to these values.
 */
sealed class RoverMode {
    abstract val displayName: String

    data object Manual       : RoverMode() { override val displayName = "Manual"       }
    data object Acro         : RoverMode() { override val displayName = "Acro"         }
    data object Steering     : RoverMode() { override val displayName = "Steering"     }
    data object Hold         : RoverMode() { override val displayName = "Hold"         }
    data object Loiter       : RoverMode() { override val displayName = "Loiter"       }
    data object Follow       : RoverMode() { override val displayName = "Follow"       }
    data object Simple       : RoverMode() { override val displayName = "Simple"       }
    data object Dock         : RoverMode() { override val displayName = "Dock"         }
    data object Circle       : RoverMode() { override val displayName = "Circle"       }
    data object Auto         : RoverMode() { override val displayName = "Auto"         }
    data object Rtl          : RoverMode() { override val displayName = "RTL"          }
    data object SmartRtl     : RoverMode() { override val displayName = "Smart RTL"   }
    data object Guided       : RoverMode() { override val displayName = "Guided"       }
    data object Initializing : RoverMode() { override val displayName = "Initializing" }

    /** Carries the raw value for any mode number ArduPilot sent that we don't recognise. */
    data class UnknownMode(val rawValue: Int) : RoverMode() {
        override val displayName = "Mode $rawValue"
    }

    companion object {
        private val knownModes: List<Pair<Int, RoverMode>> = listOf(
            0  to Manual,
            1  to Acro,
            3  to Steering,
            4  to Hold,
            5  to Loiter,
            6  to Follow,
            7  to Simple,
            8  to Dock,
            9  to Circle,
            10 to Auto,
            11 to Rtl,
            12 to SmartRtl,
            15 to Guided,
            16 to Initializing,
        )

        private val byRaw: Map<Int, RoverMode> = knownModes.toMap()

        fun fromRaw(rawValue: Int): RoverMode = byRaw[rawValue] ?: UnknownMode(rawValue)

        /** Ordered list of all known modes — use this to populate the mode picker. */
        val allKnown: List<RoverMode> = knownModes.map { it.second }

        /** Same list as (rawValue, mode) pairs — use when you need to pass rawCustomMode to ModeRepository. */
        val allKnownWithRaw: List<Pair<Int, RoverMode>> = knownModes
    }
}
