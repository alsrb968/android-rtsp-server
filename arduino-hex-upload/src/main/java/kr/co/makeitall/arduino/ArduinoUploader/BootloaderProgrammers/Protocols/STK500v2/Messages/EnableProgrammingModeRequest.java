package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Constants;
import kr.co.makeitall.arduino.ArduinoUploader.Hardware.Command;
import kr.co.makeitall.arduino.ArduinoUploader.Hardware.IMcu;

public class EnableProgrammingModeRequest extends Request {
    public EnableProgrammingModeRequest(IMcu mcu) {

        byte[] cmdBytes = mcu.getCommandBytes().get(Command.PgmEnable);
        setBytes(new byte[]{Constants.CmdEnterProgrmodeIsp, mcu.getTimeout(), mcu.getStabDelay(), mcu.getCmdExeDelay(), mcu.getSynchLoops(), mcu.getByteDelay(), mcu.getPollValue(), mcu.getPollIndex(), cmdBytes[0], cmdBytes[1], cmdBytes[2], cmdBytes[3]});
    }
}
