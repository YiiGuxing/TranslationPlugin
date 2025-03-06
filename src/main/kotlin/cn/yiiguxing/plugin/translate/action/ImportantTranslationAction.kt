package cn.yiiguxing.plugin.translate.action

/**
 * The [priority][ImportantTranslationAction.priority] value for
 * normal priority in the [ImportantTranslationAction] interface.
 */
const val ACTION_NORMAL_PRIORITY = 0

/**
 * The [priority][ImportantTranslationAction.priority] value for
 * low priority in the [ImportantTranslationAction] interface.
 * Actions should never have a priority lower than this value.
 */
const val ACTION_LOW_PRIORITY = -1000

/**
 * The [priority][ImportantTranslationAction.priority] value for
 * high priority in the [ImportantTranslationAction] interface.
 * Actions should never have a priority higher than this value.
 */
const val ACTION_HIGH_PRIORITY = 1000

interface ImportantTranslationAction {

    /**
     * The priority of the action. The value should be in the range
     * of [ACTION_LOW_PRIORITY] to [ACTION_HIGH_PRIORITY]. The default
     * value is [ACTION_NORMAL_PRIORITY].
     */
    val priority: Int get() = ACTION_NORMAL_PRIORITY

}

/**
 * Checks if the specified priority is valid.
 */
fun checkActionPriority(priority: Int) {
    check(priority in ACTION_LOW_PRIORITY..ACTION_HIGH_PRIORITY) {
        "Action priority must be in the range [$ACTION_LOW_PRIORITY, $ACTION_HIGH_PRIORITY]."
    }
}