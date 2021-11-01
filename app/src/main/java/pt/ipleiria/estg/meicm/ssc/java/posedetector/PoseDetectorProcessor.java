package pt.ipleiria.estg.meicm.ssc.java.posedetector;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;

import pt.ipleiria.estg.meicm.ssc.AppData;
import pt.ipleiria.estg.meicm.ssc.GMailSender;
import pt.ipleiria.estg.meicm.ssc.GraphicOverlay;
import pt.ipleiria.estg.meicm.ssc.java.VisionProcessorBase;
import pt.ipleiria.estg.meicm.ssc.java.posedetector.classification.PoseClassifierProcessor;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static kotlin.random.RandomKt.Random;

/** A processor to run pose detector. */
public class PoseDetectorProcessor
    extends VisionProcessorBase<PoseDetectorProcessor.PoseWithClassification> {
  private static final String TAG = "PoseDetectorProcessor";

  private final PoseDetector detector;

  private final boolean showInFrameLikelihood;
  private final boolean visualizeZ;
  private final boolean rescaleZForVisualization;
  private final boolean runClassification;
  private final boolean isStreamMode;
  private final Context context;
  private final Executor classificationExecutor;

  private PoseClassifierProcessor poseClassifierProcessor;
  /** Internal class to hold Pose and classification results. */
  protected static class PoseWithClassification {
    private final Pose pose;
    private final List<String> classificationResult;

    public PoseWithClassification(Pose pose, List<String> classificationResult) {
      this.pose = pose;
      this.classificationResult = classificationResult;
    }

    public Pose getPose() {
      return pose;
    }

    public List<String> getClassificationResult() {
      return classificationResult;
    }
  }

  public PoseDetectorProcessor(
      Context context,
      PoseDetectorOptionsBase options,
      boolean showInFrameLikelihood,
      boolean visualizeZ,
      boolean rescaleZForVisualization,
      boolean runClassification,
      boolean isStreamMode) {
    super(context);
    this.showInFrameLikelihood = showInFrameLikelihood;
    this.visualizeZ = visualizeZ;
    this.rescaleZForVisualization = rescaleZForVisualization;
    detector = PoseDetection.getClient(options);
    this.runClassification = runClassification;
    this.isStreamMode = isStreamMode;
    this.context = context;
    classificationExecutor = Executors.newSingleThreadExecutor();
  }

  @Override
  public void stop() {
    super.stop();
    detector.close();
  }

  @Override
  protected Task<PoseWithClassification> detectInImage(InputImage image) {
    return detector
        .process(image)
        .continueWith(
            classificationExecutor,
            task -> {
              Pose pose = task.getResult();
              List<String> classificationResult = new ArrayList<>();
              if (runClassification) {
                if (poseClassifierProcessor == null) {
                  poseClassifierProcessor = new PoseClassifierProcessor(context, isStreamMode);
                }
                classificationResult = poseClassifierProcessor.getPoseResult(pose);

              }
              return new PoseWithClassification(pose, classificationResult);
            });
  }

  @Override
  protected void onSuccess(
      @NonNull PoseWithClassification poseWithClassification,
      @NonNull GraphicOverlay graphicOverlay) {
    graphicOverlay.add(
        new PoseGraphic(
            graphicOverlay,
            poseWithClassification.pose,
            showInFrameLikelihood,
            visualizeZ,
            rescaleZForVisualization,
            poseWithClassification.classificationResult));

    if (poseWithClassification.classificationResult.size() > 1){
        String className = poseWithClassification.classificationResult.get(1);
        String[] classNameD = className.split(":",2);
        String[] confidenceD = classNameD[1].trim().split(" ",2);
        String confidence = confidenceD[0].trim();

        if(Double.parseDouble(confidence) > 0.75){
          className = classNameD[0].trim();
          AppData.getInstance().actualPose = className;
          Log.e("RESULT", AppData.getInstance().actualPose);
          Log.e("confidence", confidence);
          handleClassDetected(className);
        }

    }
  }

  private void handleClassDetected(String className) {
    AppData appData = AppData.getInstance();
    try {
      if (!appData.actualPose.equals(appData.previousPose)) {
        switch (appData.actualPose) {
          case "alert":
            sendMqttMsg(appData.led6, "on");
            sendMqttMsg(appData.alarmBuzz, "on");
            //TODO UNCOMMENT
            //sendEmail("nunocas3iro@gmail.com","ALERT", "PLEASE HELP");
            msgToButler("ENVIEI ALERTA PARA AS AUTORIDADES");
            break;
          case "cold":
            sendMqttMsg(appData.led1, "on");
            msgToButler("Acabei de ligar o aquecedor");
            break;
          case "hot":
            sendMqttMsg(appData.led2, "on");
            msgToButler("Acabei de ligar a ventoinha");
            break;
          case "goal":
            msgToButler("GOLO GOLO GOLO GOLO PORTUGAL");
            break;
          default:
            sendMqttMsg(appData.led6, "off");
            sendMqttMsg(appData.alarmBuzz, "off");
            sendMqttMsg(appData.led1, "off");
            sendMqttMsg(appData.led2, "off");
            appData.resetStates();
            break;
        }
      }
      appData.previousPose = className;
    } catch (Exception e) {
      Log.e("ERROR MQTT", e.getMessage());
    }

  }

  private void sendEmail(String receiverEmail, String subject, String body) throws Exception {

    new Thread(){
      public void run(){
        GMailSender sender = new GMailSender();
        try {
          sender.sendMail(subject, body, null, receiverEmail);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

    }.start();
  }





  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Pose detection failed!", e);
  }
}
