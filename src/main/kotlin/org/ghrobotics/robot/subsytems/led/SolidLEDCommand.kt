package org.ghrobotics.robot.subsytems.led

import org.ghrobotics.lib.commands.FalconCommand
import org.ghrobotics.lib.utils.setLEDOutput
import org.ghrobotics.robot.sensors.Canifier
import java.awt.Color

class SolidLEDCommand(private val color: Color) : FalconCommand(LEDSubsystem) {
    override suspend fun execute() {
        Canifier.setLEDOutput(color)
    }
}