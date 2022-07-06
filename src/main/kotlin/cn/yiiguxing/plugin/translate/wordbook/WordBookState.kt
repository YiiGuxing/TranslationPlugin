package cn.yiiguxing.plugin.translate.wordbook

enum class WordBookState {

    UNINITIALIZED,
    INITIALIZING,
    NO_DRIVER,
    DOWNLOADING_DRIVER,
    INITIALIZATION_ERROR,
    RUNNING

}