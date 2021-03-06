/*
 * FRC Team 5190
 * Green Hope Falcons
 */

package org.ghrobotics.robot.subsytems.arm

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.FeedbackDevice
import com.ctre.phoenix.motorcontrol.NeutralMode
import org.ghrobotics.lib.commands.FalconSubsystem
import org.ghrobotics.lib.mathematics.units.amp
import org.ghrobotics.lib.mathematics.units.nativeunits.fromModel
import org.ghrobotics.lib.wrappers.ctre.FalconSRX
import org.ghrobotics.robot.Constants

object ArmSubsystem : FalconSubsystem() {

    private val armMaster = FalconSRX(Constants.kArmId, Constants.kArmNativeUnitModel)

    var armPosition
        get() = armMaster.sensorPosition
        set(value) {
            var effectiveValue = value.fromModel(Constants.kArmNativeUnitModel).value
            if (effectiveValue > 0) effectiveValue -= Constants.kArmSensorUnitsPerRotation.value
            armMaster.set(ControlMode.MotionMagic, effectiveValue)
        }

    init {
        armMaster.run {
            inverted = true
            encoderPhase = false
            feedbackSensor = FeedbackDevice.Analog

            peakForwardOutput = 1.0
            peakReverseOutput = -1.0

            kP = Constants.kPArm
            kF = Constants.kVArm
            allowedClosedLoopError = Constants.kArmClosedLoopTolerance

            continuousCurrentLimit = 20.amp
            currentLimitingEnabled = true

            motionCruiseVelocity = Constants.kArmMotionMagicVelocity
            motionAcceleration = Constants.kArmMotionMagicAcceleration

            brakeMode = NeutralMode.Brake
        }

        defaultCommand = ClosedLoopArmCommand()
    }

    fun set(controlMode: ControlMode, output: Double) = armMaster.set(controlMode, output)
    override fun zeroOutputs() {
        set(ControlMode.PercentOutput, 0.0)
    }
}