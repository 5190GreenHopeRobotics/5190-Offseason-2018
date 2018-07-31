package frc.team5190.lib.commands

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.TimeUnit

open class TimeoutCommand(private val timeout: Long, private val unit: TimeUnit = TimeUnit.SECONDS) : Command() {
    private val timeoutCondition = TimeoutCondition()

    private inner class TimeoutCondition : Condition() {
        private lateinit var job: Job
        private var startTime = 0L

        fun start() {
            startTime = System.nanoTime()
            job = launch(context = CommonPool) {
                delay(timeout, unit)
                invokeCompletionListeners()
            }
        }

        fun stop() {
            job.cancel()
        }

        override suspend fun isMet() = System.nanoTime() - startTime >= unit.toNanos(timeout)
    }

    init {
        updateFrequency = 0
        finishCondition += timeoutCondition
    }

    override suspend fun initialize() {
        super.initialize()
        timeoutCondition.start()
    }

    override suspend fun dispose() {
        super.dispose()
        timeoutCondition.stop()
    }
}

open class InstantCommand : Command() {
    init {
        finishCondition += Condition.TRUE
    }
}

class InstantRunnableCommand(private val runnable: suspend () -> Unit) : InstantCommand() {
    override suspend fun initialize() {
        super.initialize()
        runnable()
    }
}

class PeriodicRunnableCommand(
        private val runnable: suspend () -> Unit,
        exitCondition: Condition,
        updateFrequency: Int = Command.DEFAULT_FREQUENCY
) : Command(updateFrequency) {
    init {
        finishCondition += exitCondition
    }

    override suspend fun execute() = runnable()
}