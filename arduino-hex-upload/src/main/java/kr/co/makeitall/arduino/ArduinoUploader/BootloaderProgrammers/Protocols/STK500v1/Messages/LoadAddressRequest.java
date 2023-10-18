package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Constants;

public class LoadAddressRequest extends Request
{
	public LoadAddressRequest(int address)
	{

		setBytes(new byte[] {Constants.CmdStkLoadAddress, (byte)(address & 0xff), (byte)((address >> 8) & 0xff), Constants.SyncCrcEop});
	}
}
