package rgpiodevice;

import java.util.Random;
import pidevice.*;

class RGPIODeviceRun implements GetCommandListener {

    DeviceInput temp;      // analog input

    static Float tempValue = 25f;

    public String onGetCommand(DeviceInput deviceInput) {
        if (deviceInput == temp) {
            return (tempValue.toString());
        }
        return ("impossible!");
    }

    public void start() {

        // pass the model and the device pins to piDevice
        // From this information a Report string can be generated
        // PiDevice will call onSetCommand() and onGetCommand() when GET or SET is received
        PiDevice.deviceModel = "RASPBERRY";
        temp = PiDevice.addAnalogInput("temp");
        temp.getCommandListener = this;

        (new SensorThread(1)).start();  // simulates changing values for temp

        PiDevice.runDevice(2600, 2500);

// todo : convert listener thread back to in line so runDevice does not exit
        try {
            Thread.sleep(999999999);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    class SensorThread extends Thread {

    // thread that simulates changing temperature and distance values
        int interval;
        GaugeSource tempSource = new GaugeSource(12345L,25f);
        
        public SensorThread(int interval) {
            super("SensorThread");
            this.interval = interval;
        }

        public void run() {

            while (true) {
                try {
                    Thread.sleep(interval * 1000);      
                    tempValue = tempSource.getValue();
                } catch (InterruptedException ie) {
                }
            }
        }
    }

class GaugeSource {

        float value;
        float slope = 0;
        int countdown = 0;
        Random RANDOM;

        GaugeSource(long seed, float value) {
            RANDOM = new Random(seed);
            this.value = value;
        }

        float getValue() {
            if (countdown == 0) {
                // new slope and countdown     
                slope = (RANDOM.nextFloat() - 0.5f);
                countdown = RANDOM.nextInt(5) + 1;
            }
            value = value + slope * 0.01f;
//           System.out.println(slope + "\t" + value);
            countdown--;
            return value;
        }

    }

}

public class RGPIODevice {

    public static void main(String[] args) {
        new RGPIODeviceRun().start();
    }
}
