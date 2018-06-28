package frc.team5190.robot

import edu.wpi.first.wpilibj.IterativeRobot
import edu.wpi.first.wpilibj.command.Scheduler
import frc.team5190.robot.drive.DriveSubsystem
import frc.team5190.robot.sensors.NavX

class Robot : IterativeRobot() {

    // Can't make entire class an object, so INSTANCE is initialized in a companion object.
    companion object {
        lateinit var INSTANCE: Robot
    }

    // Initialize instance.
    init {
        INSTANCE = this
    }

    // Initialize all systems.
    override fun robotInit() {
        PathGenerator
        Localization
        NetworkInterface
        Autonomous
        NavX

        DriveSubsystem
    }

    // Run scheduler for command based processes.
    override fun robotPeriodic() {
        Scheduler.getInstance().run()
    }

    override fun disabledInit() {}
    override fun disabledPeriodic() {}

    override fun autonomousInit() {}
    override fun autonomousPeriodic() {}

    override fun teleopInit() {}
    override fun teleopPeriodic() {}
}