package rgpiodevice;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.*;
import pidevice.*;



class RGPIODeviceRun implements SetCommandListener, GetCommandListener, GpioPinListenerDigital {

    /* This class implements a RGPIO device on Raspberry PI.
     Sending and receiving of REPORT,EVENT,GET,SET.. is handled by PiDevice().
     This class implements :
     - GpioListener to create an event when a physical GPIO input pin changes state.
     - SetCommandListener (onSetCommand()) to set a physical GPIO pin when a SET command is received.
     - GetCommandListener (onGetCommand()) to return the value of a pin on a GET command.
     */
    DeviceInput button;    // digital input
    DeviceInput distance;     // analog input
    DeviceInput temp;      // analog input

    DeviceOutput heating;   // digital output
    DeviceOutput boiler;    // digital output
    DeviceOutput pump;      // digital output

    static Float  distanceValue = 50f;
    static Float tempValue = 20f;
    static String pumpValue ="Low";
    static String heatingValue ="Low";
    static String boilerValue = "Low";
    
    // GPIO pins corresponding to the digital input and digital output
    static GpioController gpio;
    static GpioPinDigitalInput Gpio2;   // button
    static GpioPinDigitalOutput Gpio3;  // heating
    static GpioPinDigitalOutput Gpio5;  // pump
    static GpioPinDigitalOutput Gpio4;  // boiler

    public void onSetCommand(DeviceOutput deviceOutput, String newValue) {
        // set GPIO pin corresponding to this deviceOutput to newValue
        
        boolean state=false;
        if (newValue.equals("High")) state=true;
        if (newValue.equals("Low")) state=false;
        
        if (deviceOutput == heating) {
            System.out.println("GPIO 3 (heating) set to " + newValue);
            Gpio3.setState(state);
            heatingValue=newValue;
        }
        if (deviceOutput == boiler) {
            System.out.println("GPIO 4 (boiler) set to " + newValue);
            Gpio4.setState(state);
            boilerValue=newValue;
        }
        if (deviceOutput == pump) {
            System.out.println("GPIO 5 (pump) set to " + newValue);
            Gpio4.setState(state);
            pumpValue=newValue;
        }
    }

    public String onGetCommand(DeviceInput deviceInput) {
        // set GPIO pin corresponding to this deviceOutput to newValue
        if (deviceInput == button) {
            if (Gpio2.isHigh()) {
                return "High";
            }
            if (Gpio2.isLow()) {
                return "Low";
            }
            return ("?");   // impossible?
        }
        if (deviceInput == distance) {
            return (distanceValue.toString());
        }
        if (deviceInput == temp) {
            return (tempValue.toString());
        }
        return ("impossible!");
    }

    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        System.out.println("--> GPIO PIN <" + event.getPin().getName() + "> STATE CHANGE: " + event.getState());
//System.out.println("2? "+(event.getPin()==Gpio2));
//System.out.println("high? "+event.getState().isHigh());

        if (event.getState().isHigh()) {
            PiDevice.sendEvent("button", "High");
        } else {
            PiDevice.sendEvent("button", "Low");
        }
    }

    public void start() {
        gpio = GpioFactory.getInstance();
        Gpio2 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);
        Gpio3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "heating", PinState.LOW);
        Gpio4 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "boiler", PinState.LOW);
        Gpio5 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "pump", PinState.LOW);
        Gpio2.addListener(this);

        // pass the model and the device pins to piDevice
        // From this information a Report string can be generated
        // PiDevice will call onSetCommand() and onGetCommand() when GET or SET is received
        PiDevice.deviceModel = "RASPBERRY";
        button = PiDevice.addDigitalInput("button");
        heating = PiDevice.addDigitalOutput("heating");
        boiler = PiDevice.addDigitalOutput("boiler");
        pump = PiDevice.addDigitalOutput("pump");
        distance = PiDevice.addAnalogInput("distance");
        temp = PiDevice.addAnalogInput("temp");
        PiDevice.printDevicePins();

        // 'this' can execute the  SET command for outputs
        heating.setCommandListener = this;  
        boiler.setCommandListener = this;
        pump.setCommandListener = this;
        // 'this' can execute the  GET command for inputs
        button.getCommandListener = this;
        distance.getCommandListener = this;
        temp.getCommandListener = this;

        (new SensorThread(1)).start();  // simulates changing values for distance and temp

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
        // Every second:
        // temp goes up 0.1 when heating is on
        // temp goes down 0.05 when heating is off
        // distance goes up 1 when pump is on   (distance = distance to the water distance)
        // distance goes down 0.05 when pump is off
        while (true) {
            try {
                Thread.sleep(interval * 1000);
                if (heatingValue.equals("High")) tempValue+=0.1f;
                if (heatingValue.equals("Low")) tempValue-=0.05f;
                 if (pumpValue.equals("High")) distanceValue+=5f;
                if (pumpValue.equals("Low")) distanceValue-=0.2f;
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
