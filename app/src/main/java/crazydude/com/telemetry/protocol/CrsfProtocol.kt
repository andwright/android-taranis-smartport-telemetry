package crazydude.com.telemetry.protocol

import android.util.Log
import crazydude.com.telemetry.protocol.decoder.CrsfDataDecoder
import crazydude.com.telemetry.protocol.decoder.DataDecoder
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer


class CrsfProtocol : Protocol {

    constructor(dataListener: DataDecoder.Listener) : super(CrsfDataDecoder(dataListener))
    constructor(dataDecoder: DataDecoder) : super(dataDecoder)


    private var bufferIndex = 0
    private var buffer: ByteArray = ByteArray(MAX_PACKET_SIZE)
    private var state: State = Companion.State.IDLE
    private var encodedPacketLen: Byte = 0
    private var encodedCrc: Byte = 0
    private var crc = 0

    companion object {

        enum class State {
            IDLE, LENGTH, DATA
        }

        // Device:Length:Type:Payload:CRC
        private const val RADIO_ADDRESS = 0xEA
        private const val MAX_PACKET_SIZE = 62

        private const val BATTERY_TYPE = 0x08
        private const val GPS_TYPE = 0x02
        private const val FLIGHT_MODE = 0x21
    }

    private fun crc8_dvb_s2(crc: Int, a: Int): Int {
        var myCrc = crc
        myCrc = myCrc xor a
        for (i in 1..8) {
            if ((myCrc and 0x80) != 0) {
               myCrc = ((myCrc shl 1) xor 0xD5).rem(256)
            }
            else{
                myCrc = (myCrc shl 1).rem(256)
            }
        }
        return myCrc
    }

    override fun process(rxDataByte: Int) {
        when (state) {
            Companion.State.IDLE -> {
                if (rxDataByte == RADIO_ADDRESS) {
                    state = Companion.State.LENGTH
                    bufferIndex = 0
                    encodedPacketLen = 0
                    crc = 0
                }
            }
            Companion.State.LENGTH -> {
                if (rxDataByte > MAX_PACKET_SIZE) {
                    state = Companion.State.IDLE
                } else {
                    if (rxDataByte > 2) {
                        state = Companion.State.DATA
                        encodedPacketLen = rxDataByte.toByte()
                        crc = 0
                    } else {
                        state = Companion.State.IDLE
                    }
                }
            }
            Companion.State.DATA -> {
                if (bufferIndex < encodedPacketLen.toInt() - 1) {
                    buffer[bufferIndex++] = rxDataByte.toByte()
                    crc = crc8_dvb_s2(crc, rxDataByte.toInt())
                } else {
                    state = Companion.State.IDLE
                    if (bufferIndex == encodedPacketLen.toInt() - 1) {
                        encodedCrc = rxDataByte.toByte()
                        if (crc == encodedCrc.toInt()) {
                            val data = ByteBuffer.wrap(buffer, 0, encodedPacketLen.toInt() - 1)
                            try {
                                val type = data.get()
                                when (type) {
                                    BATTERY_TYPE.toByte() -> {
                                        val voltage = data.short
                                        val current = data.short
                                        val byteArray = ByteArray(3)
                                        data.get(byteArray)
                                        val byteBuffer = ByteBuffer.wrap(byteArray)
//                            val capacity = byteBuffer.int
                                        val percentage = data.get()
                                        dataDecoder.decodeData(
                                            Protocol.Companion.TelemetryData(
                                                VBAT,
                                                voltage.toInt()
                                            )
                                        )
                                        dataDecoder.decodeData(
                                            Protocol.Companion.TelemetryData(
                                                CURRENT,
                                                current.toInt()
                                            )
                                        )
//                            dataDecoder.decodeData(Protocol.Companion.TelemetryData(FUEL, capacity))
                                        dataDecoder.decodeData(
                                            Protocol.Companion.TelemetryData(
                                                FUEL,
                                                percentage.toInt()
                                            )
                                        )
                                    }
                                    GPS_TYPE.toByte() -> {
                                        val latitude = data.int
                                        val longitude = data.int
                                        val groundSpeed = data.short
                                        val heading = data.short
                                        val altitude = data.short
                                        val satellites = data.get()
                                        dataDecoder.decodeData(
                                            Protocol.Companion.TelemetryData(
                                                GPS_SATELLITES,
                                                satellites.toInt()
                                            )
                                        )
                                        dataDecoder.decodeData(
                                            Protocol.Companion.TelemetryData(
                                                GPS_LATITUDE,
                                                latitude
                                            )
                                        )
                                        dataDecoder.decodeData(
                                            Protocol.Companion.TelemetryData(
                                                GPS_LONGITUDE,
                                                longitude
                                            )
                                        )
                                        dataDecoder.decodeData(
                                            Protocol.Companion.TelemetryData(
                                                GSPEED,
                                                groundSpeed.toInt()
                                            )
                                        )
                                        dataDecoder.decodeData(
                                            Protocol.Companion.TelemetryData(
                                                HEADING,
                                                heading.toInt()
                                            )
                                        )
                                        dataDecoder.decodeData(
                                            Protocol.Companion.TelemetryData(
                                                ALTITUDE,
                                                altitude.toInt()
                                            )
                                        )
                                    }
                                    FLIGHT_MODE.toByte() -> {
                                        val byteArray = ByteArray(255)
                                        var pos = 0
                                        do {
                                            val byte = data.get()
                                            byteArray[pos] = byte
                                            pos++
                                        } while (byte != 0x00.toByte())
                                        dataDecoder.decodeData(
                                            Protocol.Companion.TelemetryData(
                                                FLYMODE,
                                                0,
                                                byteArray
                                            )
                                        )
                                    }
                                }
                            } catch (e: BufferUnderflowException) {
                                Log.d("CRSF", "BufferUnderflowException")
                            }
                        }
                    }
                }
            }
        }
    }
}