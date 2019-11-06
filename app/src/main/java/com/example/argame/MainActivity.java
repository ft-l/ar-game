package com.example.argame;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.Arrays;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private static MyArFragment AR_FRAGMENT;
    private Quaternion initialCameraQuaternion = null;
    private Plane firstPlane = null;

    private ModelRenderable treeRenderable;

    private static Random random = new Random();
    private boolean targetsPlaced = false;

    private BoundsMarker marker1;
    private BoundsMarker marker2;

    private PlayerBall ball = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_main);

        AR_FRAGMENT = (MyArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        ArSceneView view = AR_FRAGMENT.getArSceneView();

        // Scene scene = view.getScene();

        ModelRenderable.builder()
                .setSource(this, Uri.parse("tree01.sfb"))
                .build()
                .thenAccept(renderable -> treeRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Log.e(TAG, "Unable to load Renderable", throwable);
                            return null;
                });

        AR_FRAGMENT.setOnSingleTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (ball == null && marker1 != null && marker2 != null) {
                        float[] bounds = {marker1.getPosition()[0], marker2.getPosition()[0], marker1.getPosition()[2], marker2.getPosition()[2]};
                        if (bounds[0] > bounds[1]) {
                            float x = bounds[1];
                            bounds[1] = bounds[0];
                            bounds[0] = x;
                        }
                        if (bounds[2] > bounds[3]) {
                            float x = bounds[3];
                            bounds[3] = bounds[2];
                            bounds[2] = x;
                        }
                        ball = new PlayerBall(hitResult, AR_FRAGMENT, this, 0.03f, bounds);
                        Log.i(TAG, "plane polygon: " + plane.getPolygon());
                        int numOfRenderablesToPlace = random.nextInt(5);
                        for (int i = 0; i < numOfRenderablesToPlace; i++) {
                            placeRenderableInRandomPosition(plane, treeRenderable, AR_FRAGMENT, bounds);
                            placeRenderableInRandomPosition(plane, atticFanRenderable, AR_FRAGMENT, bounds);
                            placeRenderableInRandomPosition(plane, knifeRenderable, AR_FRAGMENT, bounds);
                        }
                        marker1.destroy();
                        marker2.destroy();
                    }
                });

        AR_FRAGMENT.setOnLongPressArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (marker1 == null) {
                        marker1 = new BoundsMarker(hitResult, AR_FRAGMENT, this);
                        firstPlane = plane;
                    } else if (marker2 == null) {
                        marker2 = new BoundsMarker(hitResult, AR_FRAGMENT, this, marker1);
                    }
                }
        );

        AR_FRAGMENT.setOnFlingListener(
                (float velocityX, float velocityY) -> {
                    if (ball != null) {
                        ball.setVelocity(rotateVectorByRadian(new Vector3(velocityX/100000,velocityY/100000,0),
                                getVerticalRotation(
                                        initialCameraQuaternion,
                                        AR_FRAGMENT.getArSceneView().getScene().getCamera().getWorldRotation())));
                    }
                }
        );

        initialCameraQuaternion = AR_FRAGMENT.getArSceneView().getScene().getCamera().getWorldRotation();

        AR_FRAGMENT.getArSceneView().getScene().addOnUpdateListener(this::onFrame);
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    public static void placeRenderableInRandomPosition(Plane plane, ModelRenderable renderable, MyArFragment arFragment, float[] bounds) {
        float[] randomTranslation = {
                bounds[0] + (random.nextFloat() * (bounds[1]-bounds[0])),
                plane.getCenterPose().getTranslation()[1],
                bounds[2] + (random.nextFloat() * (bounds[3]-bounds[2]))
        };

        Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(
                new Pose(randomTranslation, plane.getCenterPose().getRotationQuaternion())
        );

        Node node = new Node();
        node.setRenderable(renderable);
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.addChild(node);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
    }

    private void onFrame(FrameTime frameTime) {
        if (ball != null) {
            ball.update();
        }
        if (marker1 != null) {
            marker1.resetRotation();
        }
        if (marker2 != null) {
            marker2.resetRotation();
        }
    }

    public static double getVerticalRotation(Quaternion initialCameraQuaternion, Quaternion currentCameraQuaternion) {
        return -2 * Math.atan(((initialCameraQuaternion.w*currentCameraQuaternion.y)+(initialCameraQuaternion.x*currentCameraQuaternion.z)-(initialCameraQuaternion.y*currentCameraQuaternion.w)-(initialCameraQuaternion.z*currentCameraQuaternion.x))/
                ((initialCameraQuaternion.w*currentCameraQuaternion.w)+(initialCameraQuaternion.x*currentCameraQuaternion.x)+(initialCameraQuaternion.y*currentCameraQuaternion.y)+(initialCameraQuaternion.z*currentCameraQuaternion.z)));
    }

    public static Vector3 rotateVectorByRadian(Vector3 vector, double angleToRotateBy) {
        if (vector.x < 0) {
            return new Vector3(
                    (float) (Math.sqrt(((vector.x)*(vector.x))+((vector.y)*(vector.y)))*Math.cos(Math.atan(vector.y/vector.x)+angleToRotateBy+Math.PI)),
                    (float) (Math.sqrt(((vector.x)*(vector.x))+((vector.y)*(vector.y)))*Math.sin(Math.atan(vector.y/vector.x)+angleToRotateBy+Math.PI)),
                    vector.z);
        } else {
            return new Vector3(
                    (float) (Math.sqrt(((vector.x)*(vector.x))+((vector.y)*(vector.y)))*Math.cos(Math.atan(vector.y/vector.x)+angleToRotateBy)),
                    (float) (Math.sqrt(((vector.x)*(vector.x))+((vector.y)*(vector.y)))*Math.sin(Math.atan(vector.y/vector.x)+angleToRotateBy)),
                    vector.z);
        }
    }
}
