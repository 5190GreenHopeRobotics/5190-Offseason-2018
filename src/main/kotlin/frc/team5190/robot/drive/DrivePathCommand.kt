package frc.team5190.robot.drive

import com.ctre.phoenix.motorcontrol.ControlMode
import edu.wpi.first.wpilibj.Notifier
import edu.wpi.first.wpilibj.command.Command
import frc.team5190.lib.Pathreader
import frc.team5190.lib.control.PathFollower
import frc.team5190.robot.Localization
import frc.team5190.robot.sensors.Pigeon
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D

class DrivePathCommand(folder: String, file: String,
                       robotReversed: Boolean = false,
                       pathMirrored: Boolean = false,
                       pathReversed: Boolean = false,
                       private val resetRobotPosition: Boolean) : Command() {

    private val synchronousNotifier = Object()
    private val notifier: Notifier
    private var stopNotifier = false

    private val trajectories = Pathreader.getPaths(folder, file)

    private val pathFollower: PathFollower

    init {
        requires(DriveSubsystem)

        trajectories.forEach { trajectory ->
            if (pathReversed) {
                val reversedTrajectory = trajectory.copy()
                val distance = reversedTrajectory.segments.last().position

                reversedTrajectory.segments.reverse()
                reversedTrajectory.segments.forEach { it.position = distance - it.position }

                trajectory.segments = reversedTrajectory.segments
            }
            trajectory.segments.forEach { segment ->
                if (pathMirrored) {
                    segment.heading = -segment.heading + (2 * Math.PI)
                    segment.y = 27 - segment.y
                }
                if (robotReversed xor pathReversed) {
                    var newHeading = segment.heading + Math.PI
                    if (newHeading > 2 * Math.PI) newHeading -= 2 * Math.PI

                    segment.heading = newHeading
                }
            }
        }

        pathFollower = PathFollower(
                leftTrajectory = trajectories[0],
                rightTrajectory = trajectories[1],
                sourceTrajectory = trajectories[2],
                reversed = robotReversed).apply {

            p = 1.7
            v = 1.0 / 15.0
            vIntercept = 0.05
            a = 0.0

            pTurn = 1.0 / 80.0
        }

        notifier = Notifier {
            synchronized(synchronousNotifier) {
                if (stopNotifier) {
                    return@Notifier
                }

                val output = pathFollower.getMotorOutput(
                        robotPosition = Localization.robotPosition,
                        robotAngle = Pigeon.correctedAngle,
                        rawEncoderVelocities = DriveSubsystem.leftVelocity to DriveSubsystem.rightVelocity)

                DriveSubsystem.set(controlMode = ControlMode.PercentOutput, leftOutput = output.first, rightOutput = output.second)
            }
        }
    }

    override fun initialize() {
        if (resetRobotPosition) {
            Localization.reset(startingPosition = Vector2D(trajectories[2].segments[0].x, trajectories[2].segments[0].y))
        }

        DriveSubsystem.resetEncoders()
        notifier.startPeriodic(0.02)
    }

    override fun end() {
        synchronized(synchronousNotifier) {
            stopNotifier = true
            notifier.stop()
            DriveSubsystem.set(controlMode = ControlMode.PercentOutput, leftOutput = 0.0, rightOutput = 0.0)
        }
    }

    override fun isFinished() = pathFollower.isFinished

}