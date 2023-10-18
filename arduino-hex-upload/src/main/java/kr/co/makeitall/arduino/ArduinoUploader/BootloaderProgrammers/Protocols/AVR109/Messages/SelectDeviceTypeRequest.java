package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Constants;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;

public class SelectDeviceTypeRequest extends Request
{

	public SelectDeviceTypeRequest(byte deviceCode)
	{

		setBytes(new byte[] {Constants.CmdSelectDeviceType, deviceCode});
	}
}
