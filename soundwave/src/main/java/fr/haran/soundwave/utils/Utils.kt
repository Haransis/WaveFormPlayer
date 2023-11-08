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
import kotlin.math.abs
import kotlin.math.sqrt

object Utils {
    @SuppressLint("ConstantLocale")
    private val dateFormat = DateTimeFormatter
        .ofPattern("mm:ss")
    fun millisToString(millis:Long): String{
        return dateFormat.format(
            LocalTime.MIDNIGHT.plus(Duration.ofMillis(millis))
        )
    }

    fun List<Int>.normalize(): List<Int> {
        val average = this.average().toInt()
        val minimum = (this.minByOrNull { it } ?: 0) - average
        val maximum = (this.maxByOrNull { it } ?: 0) - average
        return this.map {
            val toreturn = it - average
            if (toreturn > 0)
                if ( toreturn > maximum/2)
                    toreturn - maximum/2
                else
                    toreturn
            else
                if ( toreturn < minimum/2)
                    toreturn - minimum/2
                else
                    toreturn
        }
    }
}