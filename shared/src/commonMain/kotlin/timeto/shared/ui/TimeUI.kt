package timeto.shared.ui

import timeto.shared.*
import timeto.shared.db.eventUiString
import kotlin.math.absoluteValue

class TimeUI(
    val unixTime: UnixTime,
    val isImportant: Boolean,
    val type: TYPE,
) {

    val daytimeText: String
    val timeLeftText: String
    val color: ColorNative
    val status: STATUS

    init {
        val secondsLeft = unixTime.time - time()
        if (secondsLeft > 0) {
            timeLeftText = secondsInToString(secondsLeft)
            color = when {
                type == TYPE.EVENT -> ColorNative.blue
                secondsLeft <= 3_600 -> ColorNative.blue
                else -> ColorNative.textSecondary
            }
        } else {
            timeLeftText = secondsOverdueToString(secondsLeft)
            color = ColorNative.red
        }

        status = when {
            secondsLeft > 3_600 -> STATUS.IN
            secondsLeft > 0 -> STATUS.NEAR
            else -> STATUS.OVERDUE
        }

        daytimeText = when (type) {
            TYPE.EVENT -> unixTime.eventUiString(withDayOfWeek3 = false)
            TYPE.REPEATING -> daytimeToString(unixTime.time - unixTime.localDayStartTime())
        }
    }

    enum class STATUS {
        IN, OVERDUE, NEAR
    }

    enum class TYPE {
        EVENT, REPEATING,
    }
}

//////

private fun secondsInToString(seconds: Int): String {
    val (h, m) = seconds.toHms(roundToNextMinute = true)
    val d = h / 24
    return when {
        d >= 1 -> "In ${d.toStringEndingDays()}"
        h >= 5 -> "In ${h.toStringEndingHours()}"
        h > 0 -> "In ${h.toStringEndingHours()}${if (m == 0) "" else " $m min"}"
        else -> "In ${m.toStringEnding(true, "minute", "min")}"
    }
}

private fun secondsOverdueToString(seconds: Int): String {
    val (h, m) = seconds.absoluteValue.toHms()
    val d = h / 24
    return when {
        d >= 1 -> d.toStringEndingDays() + " overdue"
        h > 0 -> h.toStringEndingHours() + " overdue"
        m == 0 -> "Now! 🙀"
        else -> "$m min overdue"
    }
}

private fun Int.toStringEndingHours() = toStringEnding(true, "hour", "hours")
private fun Int.toStringEndingDays() = toStringEnding(true, "day", "days")
