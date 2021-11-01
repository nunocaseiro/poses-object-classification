/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pt.ipleiria.estg.meicm.ssc.java.objectdetector;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;

import java.util.Arrays;
import java.util.List;
import pt.ipleiria.estg.meicm.ssc.AppData;
import pt.ipleiria.estg.meicm.ssc.GraphicOverlay;
import pt.ipleiria.estg.meicm.ssc.java.VisionProcessorBase;


/**
 * A processor to run object detector.
 */
public class ObjectDetectorProcessor extends VisionProcessorBase<List<DetectedObject>> {

    private static final String TAG = "ObjectDetectorProcessor";

    private final ObjectDetector detector;

    public ObjectDetectorProcessor(Context context, ObjectDetectorOptionsBase options) {
        super(context);
        detector = ObjectDetection.getClient(options);
    }

    @Override
    public void stop() {
        super.stop();
        detector.close();
    }

    @Override
    protected Task<List<DetectedObject>> detectInImage(InputImage image) {
        return detector.process(image);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onSuccess(
            @NonNull List<DetectedObject> results, @NonNull GraphicOverlay graphicOverlay) {
        for (DetectedObject object : results) {
            //graphicOverlay.add(new ObjectGraphic(graphicOverlay, object));

        Log.e("RESULTS:", String.valueOf(results.size()));
        if (!results.isEmpty() && results.get(0).getLabels().get(0).getConfidence() > 0.70) {
                String className = results.get(0).getLabels().get(0).getText();
                AppData.getInstance().actualPose = className;
                Log.e("RESULT", AppData.getInstance().actualPose);
                handleClassDetected(className);
        }
                graphicOverlay.add(new ObjectGraphic(graphicOverlay, object));
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void handleClassDetected(String className){
        AppData appData = AppData.getInstance();
        try{
            if(!appData.actualPose.equals(appData.previousPose)){

                if(appData.countForDice > 2){
                    appData.countForDice = 0;
                }

                switch (appData.actualPose){
                    case "1":
                    case "2":
                    case "3":
                    case "4":
                    case "5":
                        sendMqttMsg(appData.getId(Integer.parseInt(appData.actualPose)), "on");
                        break;
                    case "6":
                        for (int i = 1; i <= 5 ; i++) {
                            sendMqttMsg(appData.getId(i),"off");
                        }
                        sendMqttMsg(AppData.getInstance().alarmBuzz,"off");
                        appData.countForDice = 0;
                        break;
                    default:
                        Log.d("Handle class", "Default");
                        break;
                }

                if(!appData.actualPose.equals("6")){
                    appData.sequence[appData.countForDice] = Integer.parseInt(appData.actualPose);

                    appData.countForDice ++ ;

                    Log.e("ARRAY", Arrays.toString(appData.sequence));
                    if(Arrays.equals(appData.sequence,appData.sequenceToAchieve)){
                        sendMqttMsg(AppData.getInstance().alarmBuzz,"on");
                    }
                }

            }
            AppData.getInstance().previousPose = className;
        }catch (Exception e){
            Log.e("ERROR MQTT", e.getMessage());
        }

    }


    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Object detection failed!", e);
    }
}
