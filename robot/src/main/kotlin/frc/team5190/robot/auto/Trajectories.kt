/*
 * FRC Team 5190
 * Green Hope Falcons
 */

@file:Suppress("unused")

package frc.team5190.robot.auto

import frc.team5190.lib.geometry.Pose2d
import frc.team5190.lib.geometry.Pose2dWithCurvature
import frc.team5190.lib.geometry.Rotation2d
import frc.team5190.lib.geometry.Translation2d
import frc.team5190.lib.trajectory.Trajectory
import frc.team5190.lib.trajectory.TrajectoryGenerator
import frc.team5190.lib.trajectory.timing.CentripetalAccelerationConstraint
import frc.team5190.lib.trajectory.timing.TimedState
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.awaitAll
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import java.util.*

object Trajectories {

    // Constants in Feet Per Second
    private const val kMaxVelocity = 11.0
    private const val kMaxAcceleration = 6.0


    // Constraints
    private val kCentripetalConstraint =
            Arrays.asList(CentripetalAccelerationConstraint(6.0))


    // Robot Constants
    private const val kRobotWidth = 27.0 / 12.0
    private const val kRobotLength = 33.0 / 12.0
    private const val kIntakeLength = 16.0 / 12.0
    private const val kBumperLength = 02.0 / 12.0

    private val kCenterToIntake = Pose2d(Translation2d(-(kRobotLength / 2.0) - kIntakeLength, 0.0), Rotation2d())
    private val kCenterToFrontBumper = Pose2d(Translation2d(-(kRobotLength / 2.0) - kBumperLength, 0.0), Rotation2d())


    // Field Relative Constants
    internal val kSideStart = Pose2d(Translation2d((kRobotLength / 2.0) + kBumperLength, 23.5), Rotation2d.fromDegrees(180.0))
    internal val kCenterStart = Pose2d(Translation2d((kRobotLength / 2.0) + kBumperLength, 13.2), Rotation2d())

    private val kNearScaleEmpty = Pose2d(Translation2d(22.7, 20.50), Rotation2d.fromDegrees(170.0))
    private val kNearScaleFull = Pose2d(Translation2d(22.7, 20.00), Rotation2d.fromDegrees(165.0))

    private val kFarScaleEmpty = Pose2d(Translation2d(22.7, 06.50), Rotation2d.fromDegrees(190.0))
    private val kFarScaleFull = Pose2d(Translation2d(22.7, 07.00), Rotation2d.fromDegrees(195.0))

    private val kNearCube1 = Pose2d(Translation2d(16.5, 19.5), Rotation2d.fromDegrees(190.0))
    private val kNearCube2 = Pose2d(Translation2d(16.5, 17.0), Rotation2d.fromDegrees(245.0))
    private val kNearCube3 = Pose2d(Translation2d(16.5, 14.5), Rotation2d.fromDegrees(240.0))

    private val kNearCube1Adjusted = kNearCube1.transformBy(kCenterToIntake)
    private val kNearCube2Adjusted = kNearCube2.transformBy(kCenterToIntake)
    private val kNearCube3Adjusted = kNearCube3.transformBy(kCenterToIntake)

    private val kFarCube1 = Pose2d(Translation2d(16.5, 07.5), Rotation2d.fromDegrees(170.0))
    private val kFarCube2 = Pose2d(Translation2d(16.5, 10.0), Rotation2d.fromDegrees(115.0))
    private val kFarCube3 = Pose2d(Translation2d(16.5, 12.5), Rotation2d.fromDegrees(120.0))

    private val kFarCube1Adjusted = kFarCube1.transformBy(kCenterToIntake)
    private val kFarCube2Adjusted = kFarCube2.transformBy(kCenterToIntake)
    private val kFarCube3Adjusted = kFarCube3.transformBy(kCenterToIntake)

    private val kSwitchLeft = Pose2d(Translation2d(11.5, 18.8), Rotation2d())
    private val kSwitchRight = Pose2d(Translation2d(11.5, 08.2), Rotation2d())

    private val kSwitchLeftAdjusted = kSwitchLeft.transformBy(kCenterToFrontBumper)
    private val kSwitchRightAdjusted = kSwitchRight.transformBy(kCenterToFrontBumper)


    // Waypoints
    private val kStartToNearScaleWpts = mutableListOf(
            kSideStart, kSideStart.transformBy(Pose2d.fromTranslation(Translation2d(-10.0, 0.0))), kNearScaleEmpty
    )
    private val kStartToFarScaleWpts = mutableListOf(
            kSideStart,
            kSideStart.transformBy(Pose2d(Translation2d(-13.0, 00.0), Rotation2d())),
            kSideStart.transformBy(Pose2d(Translation2d(-18.3, 05.0), Rotation2d.fromDegrees(-90.0))),
            kSideStart.transformBy(Pose2d(Translation2d(-18.3, 15.0), Rotation2d.fromDegrees(-90.0))),
            kFarScaleEmpty
    )

    private val kNearScaleToCube1Wpts = mutableListOf(kNearScaleEmpty, kNearCube1Adjusted)
    private val kCube1ToNearScaleWpts = mutableListOf(kNearCube1Adjusted, kNearScaleFull)

    private val kNearScaleToCube2Wpts = mutableListOf(kNearScaleFull, kNearCube2Adjusted)
    private val kCube2ToNearScaleWpts = mutableListOf(kNearCube2Adjusted, kNearScaleFull)

    private val kNearScaleToCube3Wpts = mutableListOf(kNearScaleFull, kNearCube3Adjusted)

    private val kFarScaleToCube1Wpts = mutableListOf(kFarScaleEmpty, kFarCube1Adjusted)
    private val kCube1ToFarScaleWpts = mutableListOf(kFarCube1Adjusted, kFarScaleFull)

    private val kFarScaleToCube2Wpts = mutableListOf(kFarScaleEmpty, kFarCube2Adjusted)
    private val kCube2ToFarScaleWpts = mutableListOf(kFarCube2Adjusted, kFarScaleFull)

    private val kCenterToLeftSwitchWpts = mutableListOf(kCenterStart, kSwitchLeftAdjusted)
    private val kCenterToRightSwitchWpts = mutableListOf(kCenterStart, kSwitchRightAdjusted)

    private val kBaselineWpts = mutableListOf(
            kSideStart, kSideStart.transformBy(Pose2d(Translation2d(-10.0, 0.0), Rotation2d())))


    // Trajectories
    private val startToNearScaleTrajectory = async {
        TrajectoryGenerator.generateTrajectory("Start to Near Scale", true, kStartToNearScaleWpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }
    private val startToFarScaleTrajectory = async {
        TrajectoryGenerator.generateTrajectory("Start to Far Scale", true, kStartToFarScaleWpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }

    private val nearScaleToCube1Trajectory = async {
        TrajectoryGenerator.generateTrajectory("Near Scale to Cube 1", false, kNearScaleToCube1Wpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }
    private val cube1ToNearScaleTrajectory = async {
        TrajectoryGenerator.generateTrajectory("Cube 1 to Near Scale", true, kCube1ToNearScaleWpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }

    private val nearScaleToCube2Trajectory = async {
        TrajectoryGenerator.generateTrajectory("Near Scale to Cube 2", false, kNearScaleToCube2Wpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }
    private val cube2ToNearScaleTrajectory = async {
        TrajectoryGenerator.generateTrajectory("Cube 2 to Near Scale", true, kCube2ToNearScaleWpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }

    private val nearScaleToCube3Trajectory = async {
        TrajectoryGenerator.generateTrajectory("Near Scale to Cube 3", false, kNearScaleToCube3Wpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }

    private val farScaleToCube1Trajectory = async {
        TrajectoryGenerator.generateTrajectory("Far Scale to Cube 1", false, kFarScaleToCube1Wpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }
    private val cube1ToFarScaleTrajectory = async {
        TrajectoryGenerator.generateTrajectory("Cube 1 to Far Scale", true, kCube1ToFarScaleWpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }

    private val farScaleToCube2Trajectory = async {
        TrajectoryGenerator.generateTrajectory("Far Scale to Cube 2", false, kFarScaleToCube2Wpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }
    private val cube2ToFarScaleTrajectory = async {
        TrajectoryGenerator.generateTrajectory("Cube 2 to Far Scale", true, kCube2ToFarScaleWpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }

    private val centerToLeftSwitchTrajectory = async {
        TrajectoryGenerator.generateTrajectory("Start to Left Switch", false, kCenterToLeftSwitchWpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }
    private val centerToRightSwitchTrajectory = async {
        TrajectoryGenerator.generateTrajectory("Start to Right Switch", false, kCenterToRightSwitchWpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }

    private val baselineTrajectory = async {
        TrajectoryGenerator.generateTrajectory("Baseline", true, kBaselineWpts, kMaxVelocity, kMaxAcceleration, kCentripetalConstraint)
    }

    // Hash Map
    private val trajectories = hashMapOf(
            "Start to Near Scale" to startToNearScaleTrajectory,
            "Start to Far Scale" to startToFarScaleTrajectory,

            "Near Scale to Cube 1" to nearScaleToCube1Trajectory,
            "Cube 1 to Near Scale" to cube1ToNearScaleTrajectory,
            "Near Scale to Cube 2" to nearScaleToCube2Trajectory,
            "Cube 2 to Near Scale" to cube2ToNearScaleTrajectory,
            "Near Scale to Cube 3" to nearScaleToCube3Trajectory,

            "Far Scale to Cube 1" to farScaleToCube1Trajectory,
            "Cube 1 to Far Scale" to cube1ToFarScaleTrajectory,
            "Far Scale to Cube 2" to farScaleToCube2Trajectory,
            "Cube 2 to Far Scale" to cube2ToFarScaleTrajectory,

            "Start to Left Switch" to centerToLeftSwitchTrajectory,
            "Start to Right Switch" to centerToRightSwitchTrajectory,

            "Baseline" to baselineTrajectory
    )

    init {
        launch {
            val startTime = System.currentTimeMillis()
            trajectories.values.awaitAll()
            val elapsedTime = System.currentTimeMillis() - startTime

            println("Asynchronous Trajectory Generation of ${trajectories.size} trajectories took $elapsedTime milliseconds.")
        }
    }

    operator fun get(identifier: String): Trajectory<TimedState<Pose2dWithCurvature>> = runBlocking {
        trajectories[identifier]?.await()!!
    }
}