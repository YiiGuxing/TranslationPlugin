package cn.yiiguxing.plugin.translate.tts.microsoft


internal const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"

private const val URL_BASE = "api.msedgeservices.com/tts/cognitiveservices"
internal const val EDGE_TTS_VOICES_URL = "https://$URL_BASE/voices/list"
internal const val EDGE_TTS_WSS_URL = "wss://$URL_BASE/websocket/v1"

internal const val EDGE_TTS_NORMAL_SPEED = 0
internal const val EDGE_TTS_MIN_SPEED = -50
internal const val EDGE_TTS_MAX_SPEED = 100