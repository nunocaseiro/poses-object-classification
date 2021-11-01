package pt.ipleiria.estg.meicm.ssc;

import org.eclipse.paho.android.service.MqttAndroidClient;

import java.util.HashMap;

public class AppData {
    // static variable single_instance of type Singleton
    private static AppData instance = null;

    // variable of type String
    public String previousPose = "";
    public String actualPose = "";
    public MqttAndroidClient mqttClient = null;
    public int led6 = 1029;
    public int alarmBuzz = 1030;

    public final int led1 = 1031;
    public int led2 = 1032;
    public int led3 = 1033;
    public int led4 = 1034;
    public int led5 = 1035;

    public HashMap<String, String> states = new HashMap<>();

    public int countForDice = 0;
    public int[] sequence = new int[]{0,0,0};
    public int[] sequenceToAchieve = new int[]{1,2,3};


    // private constructor restricted to this class itself
    private AppData()
    {
       resetStates();
    }

    public int getId(int id){
        switch (id){
            case 1:
                return led1;
            case 2:
                return led2;
            case 3:
                return led3;
            case 4:
                return led4;
            case 5:
                return led5;
            case 6:
                return led6;
            case 7:
                return alarmBuzz;
        }

        return 0;

    }

    public void resetStates(){
        states.put("led1", "off");
        states.put("led2", "off");
        states.put("led3", "off");
        states.put("led4", "off");
        states.put("led5", "off");
        states.put("alarmLed", "off");
        states.put("alarmBuzz", "off");

    }

    // static method to create instance of Singleton class
    public static AppData getInstance()
    {
        if (instance == null)
            instance = new AppData();

        return instance;
    }
}
