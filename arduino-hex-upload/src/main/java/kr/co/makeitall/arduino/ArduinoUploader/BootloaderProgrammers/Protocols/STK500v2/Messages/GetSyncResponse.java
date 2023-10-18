package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Response;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Constants;

import java.io.UnsupportedEncodingException;

public class GetSyncResponse extends Response {
    public final boolean getIsInSync() {
        return getBytes().length > 1 && getBytes()[0] == Constants.CmdSignOn && getBytes()[1] == Constants.StatusCmdOk;
    }

    public final String getSignature() {
        byte signatureLength = getBytes()[2];
        byte[] signature = new byte[signatureLength];
        System.arraycopy(getBytes(), 3, signature, 0, signatureLength);

        try {
            return new String(signature, 0, signature.length, "ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
