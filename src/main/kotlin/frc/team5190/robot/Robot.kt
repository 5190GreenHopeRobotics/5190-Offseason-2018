package frc.team5190.robot

import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.IterativeRobot
import edu.wpi.first.wpilibj.command.Scheduler
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import frc.team5190.lib.util.Pathreader
import frc.team5190.robot.arm.ArmSubsystem
import frc.team5190.robot.climb.ClimbSubsystem
import frc.team5190.robot.drive.DriveSubsystem
import frc.team5190.robot.elevator.ElevatorSubsystem
import frc.team5190.robot.intake.IntakeSubsystem
import frc.team5190.robot.sensors.Canifier
import frc.team5190.robot.sensors.LEDs
import frc.team5190.robot.sensors.Pigeon

class Robot : IterativeRobot() {

    // Global Robot Variables
    var isClimbing = false
    var isAutoReady = false

    val poseNTInstance = NetworkTableInstance.getDefault().getTable("PosePlotter")

    companion object {
        lateinit var INSTANCE: Robot
    }

    init {
        INSTANCE = this
    }

    override fun robotInit() {
        Localization
        Pathreader
        Autonomous
        Canifier
        Pigeon
        LEDs

        DriveSubsystem
        ElevatorSubsystem
        IntakeSubsystem
        ClimbSubsystem
        ArmSubsystem
    }

    override fun robotPeriodic() {
        Pigeon.update()
        SmartDashboard.putNumber("Robot X", Localization.robotPosition.x)
        SmartDashboard.putNumber("Robot Y", Localization.robotPosition.y)
        SmartDashboard.putNumber("Gyro", Pigeon.correctedAngle)

        Scheduler.getInstance().run()
    }
}