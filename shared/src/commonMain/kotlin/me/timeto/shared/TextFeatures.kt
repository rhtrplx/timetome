package me.timeto.shared

import me.timeto.shared.db.ActivityDb
import me.timeto.shared.db.ChecklistDb
import me.timeto.shared.db.ShortcutModel
import kotlin.math.absoluteValue

data class TextFeatures(
    val textNoFeatures: String,
    val checklists: List<ChecklistDb>,
    val shortcuts: List<ShortcutModel>,
    val fromRepeating: FromRepeating?,
    val fromEvent: FromEvent?,
    val activity: ActivityDb?,
    val timer: Int?,
    val paused: Paused?,
    val isImportant: Boolean,
) {

    val timeData: TimeData? = when {
        fromRepeating?.time != null -> TimeData(UnixTime(fromRepeating.time), TimeData.TYPE.REPEATING, this)
        fromEvent != null -> TimeData(fromEvent.unixTime, TimeData.TYPE.EVENT, this)
        else -> null
    }

    val triggers: List<Trigger> by lazy {
        checklists.map { Trigger.Checklist(it) } +
        shortcuts.map { Trigger.Shortcut(it) }
    }

    fun textUi(
        withActivityEmoji: Boolean = true,
        withPausedEmoji: Boolean = false,
        withTimer: Boolean = true,
        timerPrefix: String = "",
    ): String {
        val a = mutableListOf(textNoFeatures)
        if (paused != null && withPausedEmoji)
            a.add(0, "⏸️")
        if (activity != null && withActivityEmoji)
            a.add(activity.emoji)
        if (timer != null && withTimer)
            a.add(timerPrefix + timer.toTimerHintNote(isShort = false))
        return a.joinToString(" ")
    }

    fun textWithFeatures(): String {
        val strings = mutableListOf(textNoFeatures.trim())
        if (checklists.isNotEmpty())
            strings.add(checklists.joinToString(" ") { "#c${it.id}" })
        if (shortcuts.isNotEmpty())
            strings.add(shortcuts.joinToString(" ") { "#s${it.id}" })
        if (fromRepeating != null)
            strings.add("#r${fromRepeating.id}_${fromRepeating.day}_${fromRepeating.time ?: ""}")
        if (fromEvent != null)
            strings.add("#e${fromEvent.unixTime.time}")
        if (activity != null)
            strings.add("#a${activity.id}")
        if (timer != null)
            strings.add("#t$timer")
        if (paused != null)
            strings.add("#paused${paused.intervalId}_${paused.timer}")
        if (isImportant)
            strings.add(isImportantSubstring)
        return strings.joinToString(" ")
    }

    // Day to sync! May be different from the real one meaning "Day Start"
    // setting. "day" is used for sorting within "Today" tasks list.
    class FromRepeating(val id: Int, val day: Int, val time: Int?)

    class FromEvent(val unixTime: UnixTime)

    class Paused(val intervalId: Int, val timer: Int)

    sealed class Trigger(
        val id: String,
        val title: String,
        val emoji: String,
        val color: ColorRgba,
    ) {

        fun performUI() {
            val _when = when (this) {
                is Checklist -> launchExDefault { checklist.performUI() }
                is Shortcut -> launchExDefault { shortcut.performUI() }
            }
        }

        class Checklist(
            val checklist: ChecklistDb
        ) : Trigger("#c${checklist.id}", checklist.name, "✅", ColorRgba.green)

        class Shortcut(
            val shortcut: ShortcutModel
        ) : Trigger("#s${shortcut.id}", shortcut.name, "↗️", ColorRgba.red)
    }

    class TimeData(
        val unixTime: UnixTime,
        val type: TYPE,
        val _textFeatures: TextFeatures,
    ) {

        val secondsLeft: Int = unixTime.time - time()

        val status: STATUS = when {
            secondsLeft > 3_600 -> STATUS.IN
            secondsLeft > 0 -> STATUS.SOON
            else -> STATUS.OVERDUE
        }

        //////

        fun timeText(): String {
            val components =
                if (unixTime.isToday())
                    listOf(UnixTime.StringComponent.hhmm24)
                else
                    listOf(
                        UnixTime.StringComponent.dayOfMonth,
                        UnixTime.StringComponent.space,
                        UnixTime.StringComponent.month3,
                        UnixTime.StringComponent.comma,
                        UnixTime.StringComponent.space,
                        UnixTime.StringComponent.hhmm24,
                    )
            return unixTime.getStringByComponents(components)
        }

        fun timeLeftText(): String {
            val isOverdue = status.isOverdue()
            val (h, m) = secondsLeft.absoluteValue.toHms(roundToNextMinute = !isOverdue)
            val d = h / 24
            val timeText = when {
                d >= 1 -> "${d}d"
                h >= 10 -> "${h}h"
                h > 0 -> "${h}h${if (m == 0) "" else " ${m}m"}"
                m == 0 -> "Now! 🙀"
                else -> "${m}m"
            }
            return if (isOverdue) timeText else "In $timeText"
        }

        enum class TYPE {

            EVENT, REPEATING;

            fun isEvent() = this == EVENT
            fun isRepeating() = this == REPEATING
        }

        enum class STATUS {

            IN, SOON, OVERDUE;

            fun isIn() = this == IN
            fun isSoon() = this == SOON
            fun isOverdue() = this == OVERDUE
        }
    }
}

fun String.textFeatures(): TextFeatures = parseLocal(this)

//////

private val checklistRegex = "#c(\\d{10})".toRegex()
private val shortcutRegex = "#s(\\d{10})".toRegex()
private val fromRepeatingRegex = "#r(\\d{10})_(\\d{5})_(\\d{10})?".toRegex()
private val fromEventRegex = "#e(\\d{10})".toRegex()
private val activityRegex = "#a(\\d{10})".toRegex()
private val timerRegex = "#t(\\d+)".toRegex()
private val pausedRegex = "#paused(\\d{10})_(\\d+)".toRegex()
private const val isImportantSubstring = "#important"

private fun parseLocal(initText: String): TextFeatures {

    var textNoFeatures = initText
    fun MatchResult.clean() {
        textNoFeatures = textNoFeatures.replace(this.value, "")
    }

    val checklists: List<ChecklistDb> = checklistRegex
        .findAll(textNoFeatures)
        .map { match ->
            val id = match.groupValues[1].toInt()
            val checklist = DI.getChecklistByIdOrNull(id) ?: return@map null
            match.clean()
            checklist
        }
        .filterNotNull()
        .toList()

    val shortcuts: List<ShortcutModel> = shortcutRegex
        .findAll(textNoFeatures)
        .map { match ->
            val id = match.groupValues[1].toInt()
            val shortcut = DI.getShortcutByIdOrNull(id) ?: return@map null
            match.clean()
            shortcut
        }
        .filterNotNull()
        .toList()

    val fromRepeating: TextFeatures.FromRepeating? = fromRepeatingRegex
        .find(textNoFeatures)?.let { match ->
            val id = match.groupValues[1].toInt()
            val day = match.groupValues[2].toInt()
            val time = match.groupValues[3].takeIf { it.isNotBlank() }?.toInt()
            match.clean()
            return@let TextFeatures.FromRepeating(id, day, time)
        }

    val fromEvent: TextFeatures.FromEvent? = fromEventRegex
        .find(textNoFeatures)?.let { match ->
            val time = match.groupValues[1].toInt()
            match.clean()
            return@let TextFeatures.FromEvent(UnixTime(time))
        }

    val activity: ActivityDb? = activityRegex
        .find(textNoFeatures)?.let { match ->
            val id = match.groupValues[1].toInt()
            val activity = DI.getActivityByIdOrNull(id) ?: return@let null
            match.clean()
            return@let activity
        }

    val timer: Int? = timerRegex
        .find(textNoFeatures)?.let { match ->
            val time = match.groupValues[1].toInt()
            match.clean()
            return@let time
        }

    val paused: TextFeatures.Paused? = pausedRegex
        .find(textNoFeatures)?.let { match ->
            val intervalId = match.groupValues[1].toInt()
            val intervalTimer = match.groupValues[2].toInt()
            match.clean()
            return@let TextFeatures.Paused(intervalId, intervalTimer)
        }

    val isImportant = isImportantSubstring in textNoFeatures
    if (isImportant)
        textNoFeatures = textNoFeatures.replace(isImportantSubstring, "")

    return TextFeatures(
        textNoFeatures = textNoFeatures.removeDuplicateSpaces().trim(),
        checklists = checklists,
        shortcuts = shortcuts,
        fromRepeating = fromRepeating,
        fromEvent = fromEvent,
        activity = activity,
        timer = timer,
        paused = paused,
        isImportant = isImportant,
    )
}
