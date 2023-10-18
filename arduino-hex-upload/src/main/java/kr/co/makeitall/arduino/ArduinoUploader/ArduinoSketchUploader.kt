package kr.co.makeitall.arduino.ArduinoUploader

import IntelHexFormatReader.HexFileReader
import IntelHexFormatReader.Model.MemoryBlock
import IntelHexFormatReader.Utils.FileLineIterable
import IntelHexFormatReader.Utils.LineReader
import android.content.Context
import android.hardware.usb.UsbManager
import csharpstyle.StringHelper
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.ArduinoBootloaderProgrammer
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Avr109BootloaderProgrammer
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Stk500V1BootloaderProgrammer
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Stk500V2BootloaderProgrammer
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.ResetBehavior.IResetBehavior
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.ResetBehavior.ResetThrough1200BpsBehavior
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.ResetBehavior.ResetThroughTogglingDtrBehavior
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.ResetBehavior.ResetThroughTogglingDtrRtsBehavior
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.SerialPortConfig
import kr.co.makeitall.arduino.ArduinoUploader.Config.Arduino
import kr.co.makeitall.arduino.ArduinoUploader.Config.Configuration
import kr.co.makeitall.arduino.ArduinoUploader.Config.McuIdentifier
import kr.co.makeitall.arduino.ArduinoUploader.Config.Protocol
import kr.co.makeitall.arduino.ArduinoUploader.Hardware.*
import kr.co.makeitall.arduino.ArduinoUploader.Help.ISerialPortStream
import kr.co.makeitall.arduino.CSharpStyle.IProgress
import java.io.File
import java.io.IOException
import java.io.Reader
import java.lang.reflect.ParameterizedType
import java.util.*

class ArduinoSketchUploader<E : ISerialPortStream?> @JvmOverloads
constructor(
    private val context: Context,
    private var inferredClass: Class<E>? = null,
    private var options: ArduinoSketchUploaderOptions?,
    _logger: IArduinoUploaderLogger? = null,
    private var progress: IProgress<Double>? = null
) {

    init {
        logger = _logger
        logger?.onInfo("Starting ArduinoSketchUploader...")
        try {
            inferredClass = genericClass
        } catch (e: ClassCastException) {
            logger?.onError("Mus created as anonymous implementation (new Generic<Integer>() {};)...", e)
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

    @get:Throws(ClassNotFoundException::class)
    val genericClass: Class<E>?
        get() {
            if (inferredClass == null) {
                val mySuperclass = javaClass.genericSuperclass
                val tType = (mySuperclass as ParameterizedType).actualTypeArguments[0]
                val className = tType.toString().split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                inferredClass = Class.forName(className) as Class<E>
            }
            return inferredClass
        }

    fun uploadSketch() {
        val hexFileName = options?.fileName
        logger?.onInfo("Starting upload process for file '$hexFileName'.")
        try {
            uploadSketch(FileLineIterable(hexFileName))
        } catch (e: RuntimeException) {
            logger?.onError(e.message, e)
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun uploadSketch(hexFile: File) {
        logger?.onInfo("Starting upload process for file '${hexFile.absoluteFile}'.")
        try {
            uploadSketch(FileLineIterable(hexFile))
        } catch (e: RuntimeException) {
            logger?.onError(e.message, e)
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun uploadSketch(reader: Reader) {
        logger?.onInfo("Starting upload process for InputStreamReader.")
        try {
            uploadSketch(LineReader(reader).readLines())
        } catch (e: RuntimeException) {
            logger?.onError(e.message, e)
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun uploadSketch(hexFileContents: Iterable<String>) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevices = usbManager.deviceList
        val portNames: MutableList<String> = ArrayList()
        for ((deviceKey, usbDevice) in usbDevices) {
            val deviceVID = usbDevice.vendorId
            val devicePID = usbDevice.productId
            val deviceName = usbDevice.deviceName
            if (deviceVID != 0x1d6b && devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003) {
                // There is a device connected to our Android device. Try to open it as a Serial Port.
                portNames.add(deviceKey)
                println("deviceKey:$deviceKey")
            }
        }
        val allPortNames = portNames.toTypedArray()
        try {
            var serialPortName = options?.portName
            val temp: Set<String> = HashSet(Arrays.asList(*allPortNames))
            val uq = temp.toTypedArray()
            val distinctPorts = listOf(*uq)
            // If we don't specify a COM port, automagically select one if there is only a
            // single match.
            val portSingleOrDefault: String? = if (distinctPorts.isNotEmpty()) distinctPorts[0] else null
            if (StringHelper.isNullOrWhiteSpace(serialPortName) && portSingleOrDefault != null) {
                logger?.onInfo("Port autoselected: $serialPortName.")
                serialPortName = distinctPorts[0]
            } else if (allPortNames.isEmpty() || !distinctPorts.contains(serialPortName)) {
                throw ArduinoUploaderException("Specified COM port name '$serialPortName' is not valid.")
            }
            logger?.onTrace("Creating serial port '$serialPortName'...")

            val model = options?.arduinoModel.toString()
            val (tempOptions) = readConfiguration()
            var modelOptions: Arduino? = null
            for (arduino in tempOptions) {
                if (arduino.model.equals(model, ignoreCase = true)) {
                    modelOptions = arduino
                    break
                }
            }
            modelOptions?.let { mo ->
                serialPortName?.let { sp ->
                    uploadSketch(hexFileContents, mo, sp)
                }
            }
        } catch (e: RuntimeException) {
            logger?.onError(e.message, e)
            throw e
        }
    }

    fun uploadSketch(hexFileContents: Iterable<String>, modelOptions: Arduino, serialPortName: String) {
        val programmer: ArduinoBootloaderProgrammer<E>

        if (modelOptions == null) {
            throw ArduinoUploaderException("Unable to find configuration for '${modelOptions.model}'!")
        }
        val mcu: IMcu = when (modelOptions.mcu) {
            McuIdentifier.AtMega1284 -> AtMega1284()
            McuIdentifier.AtMega1284P -> AtMega1284P()
            McuIdentifier.AtMega2560 -> AtMega2560()
            McuIdentifier.AtMega32U4 -> AtMega32U4()
            McuIdentifier.AtMega328P -> AtMega328P()
            McuIdentifier.AtMega168 -> AtMega168()
            else -> throw ArduinoUploaderException("Unrecognized MCU: '${modelOptions.mcu}'!")
        }
        val preOpenResetBehavior = parseResetBehavior(modelOptions.preOpenResetBehavior)
        val postOpenResetBehavior = parseResetBehavior(modelOptions.postOpenResetBehavior)
        val closeResetBehavior = parseResetBehavior(modelOptions.closeResetBehavior)
        val serialPortConfig = SerialPortConfig(
            serialPortName,
            modelOptions.baudRate,
            preOpenResetBehavior,
            postOpenResetBehavior,
            closeResetBehavior,
            modelOptions.sleepAfterOpen,
            modelOptions.readTimeout,
            modelOptions.writeTimeout
        )
        programmer = when (modelOptions.protocol) {
            Protocol.Avr109 -> {
                logger?.onInfo("Protocol.Avr109")
                Avr109BootloaderProgrammer(serialPortConfig, mcu)
            }

            Protocol.Stk500v1 -> {
                logger?.onInfo("Protocol.Stk500v1")
                Stk500V1BootloaderProgrammer(serialPortConfig, mcu)
            }

            Protocol.Stk500v2 -> {
                logger?.onInfo("Protocol.Stk500v2")
                Stk500V2BootloaderProgrammer(serialPortConfig, mcu)
            }
        }
        try {
            logger?.onInfo("Establishing memory block contents...")
            val memoryBlockContents = readHexFile(hexFileContents, mcu.flash.size)
            programmer.setClazz(inferredClass)
            programmer.setContext(context)
            programmer.Open()
            logger?.onInfo("Establishing sync...")
            programmer.EstablishSync()
            logger?.onInfo("Sync established.")
            logger?.onInfo("Checking device signature...")
            programmer.CheckDeviceSignature()
            logger?.onInfo("Device signature checked.")
            logger?.onInfo("Initializing device...")
            programmer.InitializeDevice()
            logger?.onInfo("Device initialized.")
            logger?.onInfo("Enabling programming mode on the device...")
            programmer.EnableProgrammingMode()
            logger?.onInfo("Programming mode enabled.")
            logger?.onInfo("Programming device...")
            programmer.ProgramDevice(memoryBlockContents, progress)
            logger?.onInfo("Device programmed.")
            logger?.onInfo("Verifying program...")
            programmer.VerifyProgram(memoryBlockContents, progress)
            logger?.onInfo("Verified program!")
            logger?.onInfo("Leaving programming mode...")
            programmer.LeaveProgrammingMode()
            logger?.onInfo("Left programming mode!")
        } finally {
            programmer.Close()
        }
        logger?.onInfo("All done, shutting down!")
    }

    fun uploadSketch(hexFileName: String, modelOptions: Arduino, serialPortName: String) {
        logger?.onInfo("Starting upload process for file '$hexFileName'.")
        try {
            uploadSketch(FileLineIterable(hexFileName), modelOptions, serialPortName)
        } catch (ex: RuntimeException) {
            logger?.onError(ex.message, ex)
            throw ex
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun parseResetBehavior(resetBehavior: String?): IResetBehavior? {
        if (resetBehavior == null) {
            return null
        }
        if (resetBehavior.trim { it <= ' ' }.equals("1200bps", ignoreCase = true)) {
            return ResetThrough1200BpsBehavior(inferredClass, context)
        }
        val parts = resetBehavior.split(";".toRegex()).toTypedArray()
        val numberOfParts = parts.size
        if (numberOfParts == 2 && parts[0].trim { it <= ' ' }.equals("DTR", ignoreCase = true)) {
            val flag = parts[1].trim { it <= ' ' }.equals("true", ignoreCase = true)
            return ResetThroughTogglingDtrBehavior(flag)
        }
        if (numberOfParts < 3 || numberOfParts > 4) {
            throw ArduinoUploaderException("Unexpected format ($numberOfParts parts to '$resetBehavior')!")
        }

        // Only DTR-RTS supported at this point...
        val type = parts[0]
        if (!type.equals("DTR-RTS", ignoreCase = true)) {
            throw ArduinoUploaderException("Unrecognized close reset behavior: '$resetBehavior'!")
        }

        val wait1: Int =
            try {
                parts[1].toInt()
            } catch (e: RuntimeException) {
                throw ArduinoUploaderException("Unrecognized Wait (1) in DTR-RTS: '${parts[1]}'!")
            }
        val wait2: Int =
            try {
                parts[2].toInt()
            } catch (e2: RuntimeException) {
                throw ArduinoUploaderException("Unrecognized Wait (2) in DTR-RTS: '${parts[2]}'!")
            }
        val inverted = numberOfParts == 4 && parts[3].equals("true", ignoreCase = true)
        return ResetThroughTogglingDtrRtsBehavior(wait1, wait2, inverted)
    }

    companion object {
        @JvmStatic
        var logger: IArduinoUploaderLogger? = null


        private fun readHexFile(hexFileContents: Iterable<String>, memorySize: Int): MemoryBlock? {
            try {
                val reader = HexFileReader(hexFileContents, memorySize)
                return reader.Parse()
            } catch (e: RuntimeException) {
                logger?.onError(e.message, e)
                throw e
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        private fun readConfiguration(): Configuration {
            return Configuration(
                arrayOf(
                    Arduino(
                        ArduinoModel.Leonardo.toString(),
                        McuIdentifier.AtMega32U4,
                        57600,
                        Protocol.Avr109,
                        preOpenResetBehavior = "1200bps"
                    ),
                    Arduino(
                        ArduinoModel.Mega1284.toString(),
                        McuIdentifier.AtMega1284,
                        115200,
                        Protocol.Stk500v1,
                        preOpenResetBehavior = "DTR;true",
                        closeResetBehavior = "DTR-RTS;250;50"
                    ),
                    Arduino(
                        ArduinoModel.Mega2560.toString(),
                        McuIdentifier.AtMega2560,
                        115200,
                        Protocol.Stk500v2,
                        postOpenResetBehavior = "DTR-RTS;50;250;true",
                        closeResetBehavior = "DTR-RTS;250;50;true"
                    ),
                    Arduino(
                        ArduinoModel.Micro.toString(),
                        McuIdentifier.AtMega32U4,
                        57600,
                        Protocol.Avr109,
                        preOpenResetBehavior = "1200bps"
                    ),
                    Arduino(
                        ArduinoModel.NanoR2.toString(),
                        McuIdentifier.AtMega168,
                        19200,
                        Protocol.Stk500v1,
                        preOpenResetBehavior = "DTR;true",
                        closeResetBehavior = "DTR-RTS;250;50"
                    ),
                    Arduino(
                        ArduinoModel.NanoR3.toString(),
                        McuIdentifier.AtMega328P,
                        57600,
                        Protocol.Stk500v1,
                        preOpenResetBehavior = "DTR;true",
                        closeResetBehavior = "DTR-RTS;250;50"
                    ),
                    Arduino(
                        ArduinoModel.UnoR3.toString(),
                        McuIdentifier.AtMega328P,
                        115200,
                        Protocol.Stk500v1,
                        preOpenResetBehavior = "DTR;true",
                        closeResetBehavior = "DTR-RTS;50;250;false"
                    )
                )
            )
        }

        private fun parseCloseResetBehavior(closeResetBehavior: String): IResetBehavior {
            val parts = closeResetBehavior.split(";".toRegex()).toTypedArray()
            val numberOfParts = parts.size
            if (numberOfParts < 3 || numberOfParts > 4) {
                throw ArduinoUploaderException("Unexpected format ($numberOfParts parts to '$closeResetBehavior')!")
            }
            // Only DTR-RTS supported at this point...
            val type = parts[0]
            if (!type.equals("DTR-RTS", ignoreCase = true)) {
                throw ArduinoUploaderException("Unrecognized close reset behavior: '$closeResetBehavior'!")
            }

            val wait1: Int =
                try {
                    parts[1].toInt()
                } catch (e: RuntimeException) {
                    throw ArduinoUploaderException("Unrecognized Wait (1) in DTR-RTS: '${parts[1]}'!")
                }
            val wait2: Int =
                try {
                    parts[2].toInt()
                } catch (e: RuntimeException) {
                    throw ArduinoUploaderException("Unrecognized Wait (2) in DTR-RTS: '${parts[2]}'!")
                }
            val inverted = numberOfParts == 4 && parts[3].equals("true", ignoreCase = true)
            return ResetThroughTogglingDtrRtsBehavior(wait1, wait2, inverted)
        }
    }

}
