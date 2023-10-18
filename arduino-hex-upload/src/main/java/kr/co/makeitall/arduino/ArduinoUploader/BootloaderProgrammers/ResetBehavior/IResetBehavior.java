package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.ResetBehavior;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.SerialPortConfig;
import kr.co.makeitall.arduino.ArduinoUploader.Help.ISerialPortStream;

public interface IResetBehavior
{
	ISerialPortStream Reset(ISerialPortStream serialPort, SerialPortConfig config);
}
