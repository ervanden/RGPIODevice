package rgpiodevice;

import java.util.Random;
import java.util.Scanner;
import pidevice.*;
import tcputils.TelnetCommand;

class RGPIODeviceRun implements GetCommandListener {

    DeviceInput[] pduArray = new DeviceInput[9]; // array has 0 - 8, we use 1 - 8
    static Integer[] pduValue = new Integer[9];

    DeviceInput[] tempArray = new DeviceInput[4]; // analog input
    DeviceInput[] humiArray = new DeviceInput[4];  // analog input

    static Integer[] tempValue = new Integer[4]; //2500;
    static Integer[] humiValue = new Integer[4]; //5000;

    private String integerToString(Integer i) {
        if (i != null) {
            return i.toString();
        } else {
            return "NaN";
        }
    }

    public String onGetCommand(DeviceInput deviceInput) {

        for (int i = 1; i <= 8; i++) {
            if (deviceInput == pduArray[i]) {
                return (integerToString(pduValue[i]));
            }
        }
        for (int i = 0; i < 4; i++) {
            if (deviceInput == tempArray[i]) {
                return (integerToString(tempValue[i]));
            }
            if (deviceInput == humiArray[i]) {
                return (integerToString(humiValue[i]));
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
            pduValue[i] = null;
            pduArray[i].getCommandListener = this;

        }
        for (int i = 0; i < 4; i++) {
            // creating PiDevice pins
            tempArray[i] = PiDevice.addAnalogInput("T" + i);
            humiArray[i] = PiDevice.addAnalogInput("H" + i);
            tempValue[i] = null;
            humiValue[i] = null;
            tempArray[i].getCommandListener = this;
            humiArray[i].getCommandListener = this;
        }

        (new PDUReader(5)).start();  // reads the values every x seconds
        (new SensorThread(120)).start();  // changes the temp and humidity values every x seconds

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
                            pduValue[i] = null;
                        } else {

                            pduValue[i] = Math.round(f * 1000);
                            System.out.println(">> " + f + " KWatt pduValue=" + pduValue[i]);
                        }
//            System.out.println("-----------------");
                    }

                    Thread.sleep(interval * 1000);

                } catch (InterruptedException ie) {
                }
            }
        }
    }

    class SensorThread extends Thread {

        // thread that simulates changing temperature and humidity values
        int interval;
        GaugeSource[] tempSource = new GaugeSource[4];
        GaugeSource[] humiSource = new GaugeSource[4];

        public SensorThread(int interval) {
            super("SensorThread");
            this.interval = interval;
            long seed = 12345L;
            for (int i = 0; i < 4; i++) {
                tempSource[i] = new GaugeSource(seed++, 2050);
                humiSource[i] = new GaugeSource(seed++, 5000);
            };
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(interval * 1000);
                    for (int i = 0; i < 4; i++) {
                        tempValue[i] = tempSource[i].getValue();
                        humiValue[i] = humiSource[i].getValue();
                    }
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    class GaugeSource {

        float initialValue;
        float value;
        float slope = 0;
        int countdown = 0;
        Random RANDOM;

        GaugeSource(long seed, int value) {
            RANDOM = new Random(seed);
            this.value = value;
            this.initialValue = value;
        }

        int getValue() {
            if (countdown == 0) {
                // new slope and countdown  
                float correction = 0f;
                if ((value - initialValue) > 0.1 * initialValue) {
                    correction = -0.2f;
                }
                if ((initialValue - value) > 0.1 * initialValue) {
                    correction = +0.2f;
                }
                slope = 100 * ((RANDOM.nextFloat() - 0.5f + correction));
                countdown = RANDOM.nextInt(5) + 1;
            }
            value = value + slope;
            countdown--;
            return Math.round(value);
        }

    }
}

public class RGPIODevice {

    public static void main(String[] args) {
        new RGPIODeviceRun().start();
    }
}
