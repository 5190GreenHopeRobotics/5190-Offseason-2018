/*
 * FRC Team 5190
 * Green Hope Falcons
 */

package frc.team5190.robot.subsytems.drive

import com.ctre.phoenix.motorcontrol.ControlMode
import frc.team5190.lib.commands.Command
import frc.team5190.lib.commands.Condition
import frc.team5190.lib.math.control.VelocityPIDFController
import frc.team5190.lib.math.geometry.Pose2dWithCurvature
import frc.team5190.lib.math.geometry.Translation2d
import frc.team5190.lib.math.trajectory.Trajectory
import frc.team5190.lib.math.trajectory.TrajectoryIterator
import frc.team5190.lib.math.trajectory.TrajectorySamplePoint
import frc.team5190.lib.math.trajectory.TrajectoryUtil
import frc.team5190.lib.math.trajectory.followers.NonLinearController
import frc.team5190.lib.math.trajectory.followers.TrajectoryFollower
import frc.team5190.lib.math.trajectory.timing.TimedState
import frc.team5190.lib.math.trajectory.view.TimedView
import frc.team5190.lib.utils.*
import frc.team5190.robot.Constants
import frc.team5190.robot.Kinematics
import frc.team5190.robot.Localization

class FollowTrajectoryCommand(private val trajectory: Source<Trajectory<TimedState<Pose2dWithCurvature>>>, private val pathMirrored: BooleanSource = constSource(false)) : Command() {

    constructor(trajectory: Trajectory<TimedState<Pose2dWithCurvature>>, pathMirrored: BooleanSource = constSource(false)) : this(constSource(trajectory), pathMirrored)

    private val trajectoryFinished = variableState(false)
    private lateinit var trajectoryUsed: Trajectory<TimedState<Pose2dWithCurvature>>

    // Path follower
    private lateinit var trajectoryFollower: TrajectoryFollower

    private val markerLocations = mutableListOf<Marker>()
    private val markers = mutableListOf<MarkerInternal>()

    // PIDF controllers
    private val lController: VelocityPIDFController
    private val rController: VelocityPIDFController

    private var lastVelocity = 0.0 to 0.0

    init {
        +DriveSubsystem

        // Initialize PIDF Controllers
        lController = VelocityPIDFController(
                kP = Constants.kPLeftDriveVelocity,
                kI = Constants.kILeftDriveVelocity,
                kV = Constants.kVLeftDriveVelocity,
                kA = Constants.kALeftDriveVelocity,
                kS = Constants.kSLeftDriveVelocity,
                current = { DriveSubsystem.leftVelocity.FPS }
        )

        rController = VelocityPIDFController(
                kP = Constants.kPRightDriveVelocity,
                kI = Constants.kIRightDriveVelocity,
                kV = Constants.kVRightDriveVelocity,
                kA = Constants.kARightDriveVelocity,
                kS = Constants.kSRightDriveVelocity,
                current = { DriveSubsystem.rightVelocity.FPS }
        )

        // Update the frequency of the command to the follower
        updateFrequency = 100 // Hz

        trajectoryFinished.value = false
        finishCondition += trajectoryFinished
    }

    override suspend fun initialize() {
        trajectoryUsed = this.trajectory.value

        // Add markers
        markers.clear()
        // Iterate through the trajectory and add a data point every 50 ms.
        val iterator = TrajectoryIterator(TimedView(trajectoryUsed))
        val dataArray = arrayListOf<TrajectorySamplePoint<TimedState<Pose2dWithCurvature>>>()

        while (!iterator.isDone) {
            dataArray.add(iterator.advance(0.05))
        }
        markerLocations.forEach { marker ->
            val condition = marker.condition as VariableState<Boolean>
            condition.value = false // make sure its false

            val usedLocation = marker.location.value
            markers.add(MarkerInternal(dataArray.minBy { usedLocation.distance(it.state.state.translation) }!!.state.t, condition))
        }

        val finalTrajectory = if (pathMirrored.value) {
            TrajectoryUtil.mirrorTimed(trajectoryUsed)
        } else {
            trajectoryUsed
        }

        // Initialize path follower
        trajectoryFollower = NonLinearController(finalTrajectory)
        trajectoryFinished.value = false
    }

    fun addMarkerAt(location: Translation2d) = addMarkerAt(constSource(location))
    fun addMarkerAt(location: Source<Translation2d>) = Marker(location, variableState(false)).also { markerLocations.add(it) }

    private fun updateDashboard() {
        pathX = trajectoryFollower.pose.translation.x
        pathY = trajectoryFollower.pose.translation.y
        pathHdg = trajectoryFollower.pose.rotation.radians

        lookaheadX = pathX
        lookaheadY = pathY
    }

    override suspend fun execute() {
        val position = Localization.robotPosition
        val kinematics = trajectoryFollower.getSteering(position)
        val output = Kinematics.inverseKinematics(kinematics)

        DriveSubsystem.set(ControlMode.PercentOutput,
                lController.getPIDFOutput(output.first to (output.first - lastVelocity.first) * updateFrequency),
                rController.getPIDFOutput(output.second to (output.second - lastVelocity.second) * updateFrequency))


        updateDashboard()

        System.out.printf(
                "RX: %3.3f, RY: %3.3f, RA: %2f, RLV: %2.3f, RRV: %2.3f, " +
                        "AX: %3.3f, AY: %3.3f, AA: %2f, ALV: %2.3f, ARV: %2.3f%n",
                pathX, pathY, Math.toDegrees(pathHdg), output.first, output.second,
                position.translation.x, position.translation.y, position.rotation.degrees,
                DriveSubsystem.leftVelocity.FPS, DriveSubsystem.rightVelocity.FPS
        )

        // Update marker states
        val followerStateTime = trajectoryFollower.point.state.t
        markers.forEach { it.condition.value = followerStateTime > it.t }

        trajectoryFinished.value = trajectoryFollower.isFinished

        lastVelocity = output
    }

    override suspend fun dispose() {
        DriveSubsystem.set(controlMode = ControlMode.PercentOutput, leftOutput = 0.0, rightOutput = 0.0)
        println("[Trajectory Follower] " +
                "Norm of Translational Error: " +
                "${(Localization.robotPosition.translation - trajectoryUsed.lastState.state.translation).norm}")
        println("[Trajectory Follower]" +
                "Rotation Error: " +
                "${(Localization.robotPosition.rotation - trajectoryUsed.lastState.state.rotation).degrees} degrees.")

    }

    companion object {
        var pathX = 0.0
            private set
        var pathY = 0.0
            private set
        var pathHdg = 0.0
            private set

        var lookaheadX = 0.0
            private set
        var lookaheadY = 0.0
            private set
    }

    class Marker(val location: Source<Translation2d>, val condition: Condition)
    private class MarkerInternal(val t: Double, val condition: VariableState<Boolean>)
}