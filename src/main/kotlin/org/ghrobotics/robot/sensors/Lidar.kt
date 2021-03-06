/*
 * FRC Team 5190
 * Green Hope Falcons
 */

package org.ghrobotics.robot.sensors

import com.ctre.phoenix.CANifier
import com.kauailabs.navx.frc.AHRS
import edu.wpi.first.wpilibj.Servo
import kotlinx.coroutines.GlobalScope
import openrio.powerup.MatchData
import org.apache.commons.math3.stat.regression.SimpleRegression
import org.ghrobotics.lib.mathematics.twodim.geometry.Pose2d
/* ktlint-disable no-wildcard-imports */
import org.ghrobotics.lib.mathematics.units.*
import org.ghrobotics.lib.utils.Source
import org.ghrobotics.lib.utils.launchFrequency
import org.ghrobotics.lib.wrappers.FalconRobotBase
import org.ghrobotics.robot.Constants
import org.ghrobotics.robot.auto.Autonomous
import org.ghrobotics.robot.subsytems.drive.DriveSubsystem

object Lidar : Source<Pair<Boolean, Length>> {

    private val pwmData = DoubleArray(2)
    private var servo = Servo(Constants.kLidarServoId)
    private val regressionFunction = SimpleRegression()

    private val kNearScaleFull = Pose2d(23.95.feet, 20.2.feet, 160.degree)

    // All in inches
    private val kMinScaleHeight = 48.inch
    private val kMaxScaleHeight = 72.inch
    private val kAllowedTolerance = 3.inch

    var underScale = false
        private set

    var scaleHeight = 0.inch
        private set

    override fun invoke() = underScale to scaleHeight

    init {
        // X - Raw Sensor Units
        // Y - Height in Inches
        val data = arrayOf(
                1050.0 to 45.0,
                1500.0 to 55.0,
                1900.0 to 70.0
        )

        data.forEach { regressionFunction.addData(it.first, it.second) }

        GlobalScope.launchFrequency { run() }
    }

    private fun run() {
        Canifier.getPWMInput(CANifier.PWMChannel.PWMChannel0, pwmData)

        val rawDistance = pwmData[0]
        scaleHeight = regressionFunction.predict(rawDistance).inch
        underScale = kMinScaleHeight - kAllowedTolerance < scaleHeight && scaleHeight < kMaxScaleHeight +
                kAllowedTolerance

        servo.angle = if (FalconRobotBase.INSTANCE.isOperatorControl) 90.0 else {
            val robotPosition = DriveSubsystem.localization()
            val scalePosition =
                    kNearScaleFull.let { if (Autonomous.Config.scaleSide() == MatchData.OwnedSide.RIGHT) it.mirror else it }
            val angle = (scalePosition.translation - robotPosition.translation).let {
                Rotation2d(it.x.value, it.y.value, true)
            } + 180.degree + robotPosition.rotation

            angle.degree
        }
    }
}
