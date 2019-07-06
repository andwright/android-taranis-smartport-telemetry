package crazydude.com.telemetry.protocol

import androidx.annotation.IntDef
import crazydude.com.telemetry.protocol.decoder.DataDecoder

abstract class Protocol(val dataDecoder: DataDecoder) {

    companion object {

        const val FUEL = 0
        const val GPS = 1
        const val VBAT = 2
        const val CELL_VOLTAGE = 3
        const val CURRENT = 4
        const val HEADING = 5
        const val RSSI = 6
        const val FLYMODE = 7
        const val GPS_STATE = 8
        const val VSPEED = 9
        const val ALTITUDE = 10
        const val GSPEED = 11
        const val DISTANCE = 12
        const val ROLL = 13
        const val PITCH = 14
        const val GALT = 15
        const val ASPEED = 16
        const val GPS_LATITUDE = 17
        const val GPS_LONGITUDE = 18
        const val GPS_SATELLITES = 19

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            FUEL,
            GPS,
            VBAT,
            CELL_VOLTAGE,
            CURRENT,
            HEADING,
            RSSI,
            FLYMODE,
            GPS_STATE,
            VSPEED,
            ALTITUDE,
            GSPEED,
            DISTANCE,
            ROLL,
            PITCH,
            GALT,
            ASPEED
        )
        annotation class TelemetryType

        data class TelemetryData(@TelemetryType val telemetryType: Int, val data: Int)
    }

    abstract fun process(data: Int)
}