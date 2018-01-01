package rgpiodevice;

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

    public SensorThread(int interval) {
        super("SensorThread");
        this.interval = interval;
    }

    public void run() {
        float tempIncrement=0.1f;
        while (true) {
            try {
                Thread.sleep(interval * 1000);
                if (tempValue>30f) tempIncrement=-1f; //-0.1f;
                if (tempValue<20f) tempIncrement=1f;  //+0.1f;
                tempValue=tempValue+tempIncrement;
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
