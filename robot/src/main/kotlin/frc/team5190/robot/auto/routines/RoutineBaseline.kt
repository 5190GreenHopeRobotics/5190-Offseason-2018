/*
 * FRC Team 5190
 * Green Hope Falcons
 */

package frc.team5190.robot.auto.routines

import edu.wpi.first.wpilibj.command.CommandGroup
import frc.team5190.lib.extensions.sequential
import frc.team5190.robot.subsytems.drive.FollowTrajectoryCommand

class RoutineBaseline : BaseRoutine() {
    override val routine: CommandGroup
        get() {
            return sequential {
                add(FollowTrajectoryCommand(
                        identifier = "Baseline",
                        pathMirrored = false,
                        resetRobotPosition = true))
            }
        }
}