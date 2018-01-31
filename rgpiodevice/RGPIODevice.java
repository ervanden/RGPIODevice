package rgpiodevice;

import java.util.Random;
import pidevice.*;

class RGPIODeviceRun implements GetCommandListener {

    DeviceInput[] tempArray = new DeviceInput[4]; // analog input
    DeviceInput[] humiArray = new DeviceInput[4];  // analog input

    static Integer[] tempValue = new Integer[4]; //2500;
    static Integer[] humiValue = new Integer[4]; //5000;

    public String onGetCommand(DeviceInput deviceInput) {
        System.out.println("receiving get command for "+deviceInput.name);
        for (int i = 0; i < 4; i++) {
            if (deviceInput == tempArray[i]) {
                return (tempValue[i].toString());
            }
            if (deviceInput == humiArray[i]) {
                return (humiValue[i].toString());
            }
        }
        return ("impossible!");
    }

    public void start() {

        // pass the model and the device pins to piDevice
        // From this information a Report string can be generated
        // PiDevice will call onSetCommand() and onGetCommand() when GET or SET is received
        PiDevice.deviceModel = "RASPBERRY";
        for (int i = 0; i < 4; i++) {
            System.out.println("creating PiDevice pins");
            tempArray[i] = PiDevice.addAnalogInput("T" + i);
            humiArray[i] = PiDevice.addAnalogInput("H" + i);
            tempValue[i]= new Integer(0);
            humiValue[i]= new Integer(0);
            tempArray[i].getCommandListener = this;
            humiArray[i].getCommandListener = this;
                        System.out.println("done creating PiDevice pins");
        }
        (new SensorThread(2)).start();  // changes the values every 2 seconds

        PiDevice.runDevice(2600, 2500);

// todo : convert listener thread back to in line so runDevice does not exit
        try {
            Thread.sleep(999999999);
        } catch (Exception e) {
            e.printStackTrace();
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
            long seed=12345L;
            for (int i = 0; i < 4; i++) {
                tempSource[i] = new GaugeSource(seed++, 2500);
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
                if ((value - initialValue) > 0.2 * initialValue) {
                    correction = -0.2f;
                }
                if ((initialValue - value) > 0.2 * initialValue) {
                    correction = +0.2f;
                }
                slope = 100 * ((RANDOM.nextFloat() - 0.5f + correction));
                countdown = RANDOM.nextInt(5) + 1;
            }
            value = value + slope;
//           System.out.println(slope + "\t" + value);
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
