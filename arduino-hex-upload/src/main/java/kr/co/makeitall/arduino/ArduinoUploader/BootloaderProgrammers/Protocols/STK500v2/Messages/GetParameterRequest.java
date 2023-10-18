package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Constants;

public class GetParameterRequest extends Request
{

	public GetParameterRequest(byte param)
	{

		setBytes(new byte[] {Constants.CmdGetParameter, param});
	}
}
