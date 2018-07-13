/*
 * FRC Team 5190
 * Green Hope Falcons
 */

package frc.team5190.lib.extensions

import java.security.MessageDigest

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digested = md.digest(toByteArray())
    return digested.joinToString("") {
        String.format("%02x", it)
    }
}