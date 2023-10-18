package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Constants;

public class ReadSignatureRequest extends Request
{
	public ReadSignatureRequest()
	{

		setBytes(new byte[] {Constants.CmdStkReadSignature, Constants.SyncCrcEop});
	}
}
