package frc.team5190.lib.commands

import java.util.concurrent.atomic.AtomicLong

abstract class Subsystem(@Suppress("unused") val name: String) {
    companion object {
        private val subsystemId = AtomicLong()
    }

    constructor() : this("Subsystem ${subsystemId.incrementAndGet()}")

    var defaultCommand: Command? = null
        protected set
}