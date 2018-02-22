package rgpiodevice;

import java.util.Scanner;
import pidevice.*;
import tcputils.TelnetCommand;

class RGPIODeviceRun implements GetCommandListener {

    DeviceInput[] pduArray = new DeviceInput[9]; // array has 0 - 8, we use 1 - 8
    static Integer[] pduValue = new Integer[9];

    public String onGetCommand(DeviceInput deviceInput) {
        for (int i = 1; i <= 8; i++) {
            if (deviceInput == pduArray[i]) {
                return (pduValue[i].toString());
            }
        }
        return ("impossible!");
    }

    public void start() {

        // pass the model and the device pins to piDevice
        // From this information a Report string can be generated
        // PiDevice will call onSetCommand() and onGetCommand() when GET or SET is received
        PiDevice.deviceModel = "RASPBERRY";
        // create PiDevice pins
        for (int i = 1; i <= 8; i++) {
            pduArray[i] = PiDevice.addAnalogInput("PDU" + i);
            pduValue[i] = 0;
            pduArray[i].getCommandListener = this;

        }
        (new PDUReader(30)).start();  // changes the values every x seconds

        PiDevice.runDevice(2600, 2500);

// todo : convert listener thread back to in line so runDevice does not exit
        try {
            Thread.sleep(999999999);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    Float firstFloat(String s) {
        Float f = null;
        Scanner scanner = new Scanner(s).useDelimiter("[\r\n ]+");
        // skip everything that is not float 
        while (!scanner.hasNextFloat() && scanner.hasNext()) {
            scanner.next();
        }
        if (scanner.hasNextFloat()) {
            f = scanner.nextFloat();
        }
        scanner.close();
        return f;
    }

    class PDUReader extends Thread {

        // thread that reads the PDU's every interval seconds
        int interval;

        public PDUReader(int interval) {
            super("PDUReader");
            this.interval = interval;
        }

        public void run() {

            while (true) {
                try {

                    TelnetCommand tc = new TelnetCommand();

                    tc.remoteport = 23;
                    tc.userprompt = "User Name : ";
                    tc.username = "apc";
                    tc.passwordprompt = "Password  : ";
                    tc.password = "apc";
                    tc.commandprompt = "apc>";

                    for (int i = 1; i <= 8; i++) {
                        tc.remoteip = "172.68.8.4" + i;
                        System.out.println("querying " + tc.remoteip);
                        String result = tc.session("devReading power");
//            System.out.println(result);
//            System.out.println("-----------------");
                        Float f = firstFloat(result);
                        if (f == null) {
                            System.out.println(">> " + "NO RESULT");
                            pduValue[i] = 0;
                        } else {
                            System.out.println(">> " + f + "KWatt");
                            pduValue[i] = Math.round(f * 100);
                        }
//            System.out.println("-----------------");
                    }
                    
                    Thread.sleep(interval * 1000);

                } catch (InterruptedException ie) {
                }
            }
        }
    }

}

public class RGPIODevice {

    public static void main(String[] args) {
        new RGPIODeviceRun().start();
    }
}
