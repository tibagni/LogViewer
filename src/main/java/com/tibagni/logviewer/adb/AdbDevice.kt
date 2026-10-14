package com.tibagni.logviewer.adb

data class AdbDevice(
  val serialNumber: String,
  val state: DeviceState,
  val product: String? = null,
  val model: String? = null,
  val device: String? = null,
  val transportId: String? = null
) {
  val isOnline: Boolean get() = state == DeviceState.ONLINE

  val displayName: String
    get() {
      val readableName = (model ?: product ?: device)?.replace('_', ' ')
      val stateSuffix = if (state != DeviceState.ONLINE) " [${state.name.lowercase()}]" else ""
      return if (readableName != null) {
        "$readableName ($serialNumber)$stateSuffix"
      } else {
        "$serialNumber$stateSuffix"
      }
    }

  override fun toString(): String = displayName
}

enum class DeviceState {
  ONLINE,
  OFFLINE,
  UNAUTHORIZED,
  BOOTLOADER,
  RECOVERY,
  SIDELOAD,
  UNKNOWN;

  companion object {
    fun fromString(value: String): DeviceState = when (value.trim().lowercase()) {
      "device" -> ONLINE
      "offline" -> OFFLINE
      "unauthorized" -> UNAUTHORIZED
      "bootloader" -> BOOTLOADER
      "recovery" -> RECOVERY
      "sideload" -> SIDELOAD
      else -> UNKNOWN
    }
  }
}
