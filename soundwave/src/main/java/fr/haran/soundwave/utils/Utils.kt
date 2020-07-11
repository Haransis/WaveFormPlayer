@file:JvmName("Utils")
@file:JvmMultifileClass

package fr.haran.soundwave.utils

import java.text.SimpleDateFormat
import java.util.*

object Utils {
    val dateFormat = SimpleDateFormat("mm:ss", Locale.FRANCE)
    val date = Date(0)
    fun millisToString(millis:Long): String{
        return dateFormat.format(
            date.apply { time = millis })
    }
}