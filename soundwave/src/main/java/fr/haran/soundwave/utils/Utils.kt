@file:JvmName("Utils")
@file:JvmMultifileClass

package fr.haran.soundwave.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

object Utils {
    @SuppressLint("ConstantLocale")
    private val dateFormat = DateTimeFormatter
        .ofPattern("mm:ss")
    fun millisToString(millis:Long): String{
        return dateFormat.format(
            LocalTime.MIDNIGHT.plus(Duration.ofMillis(millis))
        )
    }
}