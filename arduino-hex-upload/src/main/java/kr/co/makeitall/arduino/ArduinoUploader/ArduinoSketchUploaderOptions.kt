package kr.co.makeitall.arduino.ArduinoUploader

import kr.co.makeitall.arduino.ArduinoUploader.Hardware.ArduinoModel

class ArduinoSketchUploaderOptions (
    var fileName: String? = null,
    var portName: String? = null,
    var arduinoModel: ArduinoModel = ArduinoModel.values()[0]
)
