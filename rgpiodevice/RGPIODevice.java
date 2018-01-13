package rgpiodevice;

import java.util.Random;
import pidevice.*;

class RGPIODeviceRun implements GetCommandListener {

    DeviceInput temp;      // analog input
    DeviceInput humi;  // analog input

    static Integer tempValue = 2500;
    static Integer humiValue = 5000;

    public String onGetCommand(DeviceInput deviceInput) {
        if (deviceInput == temp) {
            return (tempValue.toString());
        }
        if (deviceInput == humi) {
            return (humiValue.toString());
        }
        return ("impossible!");
    }

    public void start() {

        // pass the model and the device pins to piDevice
        // From this information a Report string can be generated
        // PiDevice will call onSetCommand() and onGetCommand() when GET or SET is received
        PiDevice.deviceModel = "RASPBERRY";
        temp = PiDevice.addAnalogInput("temperature");
        humi = PiDevice.addAnalogInput("humidity");
        temp.getCommandListener = this;
        humi.getCommandListener = this;

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
        GaugeSource tempSource = new GaugeSource(12345L, tempValue);
        GaugeSource humiSource = new GaugeSource(67890L, humiValue);

        public SensorThread(int interval) {
            super("SensorThread");
            this.interval = interval;
        }

        public void run() {

            while (true) {
                try {
                    Thread.sleep(interval * 1000);
                    tempValue = tempSource.getValue();
                    humiValue = humiSource.getValue();
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
