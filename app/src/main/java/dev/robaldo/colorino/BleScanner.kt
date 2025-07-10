package dev.robaldo.colorino

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log

class BLEScanner(context: Context) {
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter = bluetoothManager?.adapter
    private val scanner = bluetoothAdapter?.bluetoothLeScanner

    private var callback: ((ColorinoColour) -> Unit)? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.name == "cino") {
                result.scanRecord?.bytes?.let { raw ->
                    Log.d(
                        "BLE",
                        raw.joinToString(" ") { it.toUByte().toString(16).padStart(2, '0') })

                    val payload = parseManufacturerData(raw)
                    payload?.let { callback?.invoke(it) }
                }
            }
        }
    }

    @Throws(SecurityException::class)
    fun start(onColorFound: (ColorinoColour) -> Unit) {
        callback = onColorFound
        scanner?.startScan(scanCallback)
    }

    @Throws(SecurityException::class)
    fun stop() {
        scanner?.stopScan(scanCallback)
    }

    private fun parseManufacturerData(raw: ByteArray): ColorinoColour? {
        var i = 0
        while (i < raw.size) {
            val length = raw[i].toInt() and 0xFF
            if (length == 0 || i + length >= raw.size) break

            val type = raw[i + 1].toInt() and 0xFF
            if (type == 0xFF) {
                val dataStart = i + 2
                val dataEnd = i + length
                if (dataEnd >= raw.size) break

                // First two bytes: manufacturer ID
                val manufacturerId = (raw[dataStart + 1].toInt() and 0xFF shl 8) or (raw[dataStart].toInt() and 0xFF)
                if (manufacturerId != 0xFFFF) {
                    i += length + 1
                    continue
                }

                // Remaining payload
                val payloadBytes = raw.copyOfRange(dataStart + 2, dataEnd + 1)

                val payloadString = try {
                    payloadBytes.toString(Charsets.UTF_8)
                } catch (_: Exception) {
                    return null
                }

                val parts = payloadString.split(';')
                if (parts.size != 3) return null

                return try {
                    val r = parts[0].trim().toInt()
                    val g = parts[1].trim().toInt()
                    val b = parts[2].trim().toInt()
                    ColorinoColour(r, g, b)
                } catch (e: NumberFormatException) {
                    null
                }
            }
            i += length + 1
        }
        return null
    }
}