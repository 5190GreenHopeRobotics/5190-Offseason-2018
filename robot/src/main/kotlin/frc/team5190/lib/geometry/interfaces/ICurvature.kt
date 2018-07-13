/*
 * FRC Team 5190
 * Green Hope Falcons
 */

/*
 * Some implementations and algorithms borrowed from:
 * NASA Ames Robotics "The Cheesy Poofs"
 * Team 254
 */

package frc.team5190.lib.geometry.interfaces

interface ICurvature<S> : State<S> {
    val curvature: Double
    val dkds: Double
}