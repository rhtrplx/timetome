package timeto.shared.vm

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timeto.shared.*
import timeto.shared.db.EventModel
import timeto.shared.db.RepeatingModel
import timeto.shared.db.TaskFolderModel
import timeto.shared.db.TaskModel
import timeto.shared.vm.ui.sortedByFolder

// todo live tmrw data?
class TasksListVM(
    val folder: TaskFolderModel,
) : __VM<TasksListVM.State>() {

    data class State(
        val tasksUI: List<TaskUI>,
        val tmrwData: TmrwData?,
        val addFormInputTextValue: String,
    )

    override val state = MutableStateFlow(
        State(
            tasksUI = DI.tasks.toUiList(),
            tmrwData = if (folder.isTmrw)
                prepTmrwData(
                    allRepeatings = DI.repeatings,
                    allEvents = DI.events,
                ) else null,
            addFormInputTextValue = "",
        )
    )

    override fun onAppear() {
        val scope = scopeVM()
        TaskModel.getAscFlow()
            .onEachExIn(scope) { list ->
                state.update { it.copy(tasksUI = list.toUiList()) }
            }
        // To update daytime badges
        scope.launch {
            while (true) {
                delayToNextMinute()
                state.update { it.copy(tasksUI = TaskModel.getAsc().toUiList()) }
            }
        }
    }

    fun setAddFormInputTextValue(text: String) = state.update {
        it.copy(addFormInputTextValue = text)
    }

    fun isAddFormInputEmpty() = state.value.addFormInputTextValue.isBlank()

    fun addTask(
        onSuccess: () -> Unit,
    ) = scopeVM().launchEx {
        try {
            TaskModel.addWithValidation(state.value.addFormInputTextValue, folder)
            setAddFormInputTextValue("")
            onSuccess()
        } catch (e: UIException) {
            showUiAlert(e.uiMessage)
        }
    }

    private fun List<TaskModel>.toUiList() = this
        .filter { it.folder_id == folder.id }
        .sortedByFolder(folder)
        .map { TaskUI(it) }

    ///
    ///

    class TmrwData(
        val tasksUI: List<TmrwTaskUI>,
        val curTimeString: String,
    )

    class TmrwTaskUI(
        val task: TaskModel
    ) {
        val textFeatures = task.text.textFeatures()
        val text = textFeatures.textUi()
    }

    ///

    class TaskUI(
        val task: TaskModel,
    ) {

        val textFeatures = task.text.textFeatures()
        val text = textFeatures.textUi()

        fun start(
            onStarted: () -> Unit,
            needSheet: () -> Unit, // todo data for sheet
        ) {
            val autostartData = taskAutostartData(task) ?: return needSheet()
            launchExDefault {
                task.startInterval(
                    deadline = autostartData.second,
                    activity = autostartData.first,
                )
                onStarted()
            }
        }

        fun upFolder(newFolder: TaskFolderModel) {
            launchExDefault {
                task.upFolder(newFolder, replaceIfTmrw = true)
            }
        }

        // - By manual removing;
        // - After adding to calendar;
        // - By starting from activity sheet;
        fun delete() {
            launchExDefault {
                task.delete()
            }
        }
    }
}

private fun prepTmrwData(
    allRepeatings: List<RepeatingModel>,
    allEvents: List<EventModel>,
): TasksListVM.TmrwData {

    val rawTasks = mutableListOf<TaskModel>()
    val unixTmrwDS = UnixTime(utcOffset = localUtcOffsetWithDayStart).inDays(1)
    val tmrwDSDay = unixTmrwDS.localDay
    var lastFakeTaskId = unixTmrwDS.localDayStartTime()

    // Repeatings
    allRepeatings
        .filter { it.getNextDay() == tmrwDSDay }
        .forEach { repeating ->
            rawTasks.add(
                TaskModel(
                    id = ++lastFakeTaskId,
                    text = repeating.prepTextForTask(tmrwDSDay),
                    folder_id = TaskFolderModel.ID_TODAY,
                )
            )
        }

    // Events
    allEvents
        .filter { it.getLocalTime().localDay == tmrwDSDay }
        .forEach { event ->
            rawTasks.add(
                TaskModel(
                    id = ++lastFakeTaskId,
                    text = event.prepTextForTask(),
                    folder_id = TaskFolderModel.ID_TODAY,
                )
            )
        }

    val resTasks = rawTasks
        .sortedByFolder(DI.getTodayFolder())
        .map { TasksListVM.TmrwTaskUI(it) }

    val curTimeString = unixTmrwDS.getStringByComponents(
        listOf(
            UnixTime.StringComponent.dayOfMonth,
            UnixTime.StringComponent.space,
            UnixTime.StringComponent.month,
            UnixTime.StringComponent.comma,
            UnixTime.StringComponent.space,
            UnixTime.StringComponent.dayOfWeek,
        )
    )

    return TasksListVM.TmrwData(
        tasksUI = resTasks,
        curTimeString = "Tomorrow, $curTimeString"
    )
}
