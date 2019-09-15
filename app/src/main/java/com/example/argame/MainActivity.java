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
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Sphere;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private MyArFragment arFragment;
    private ModelRenderable redSphereRenderable;
    private ModelRenderable greenCubeRenderable;
    private ModelRenderable blueCylinderRenderable;
    private ModelRenderable treeRenderable;

    private Plane firstPlane = null;
    private static Random random = new Random();
    private boolean targetsPlaced = false;
    private AnchorNode targetAnchor = null;
    private TransformableNode targetNode = null;
    private float[] translation = {0f, 0f, 0f};
    private float[] rotation = {0.05f, 0f, 0f, 0f};
    private int angle = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_main);

        arFragment = (MyArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        ArSceneView view = arFragment.getArSceneView();

        Scene scene = view.getScene();

//        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
//                .thenAccept(
//                        material -> {
//                            redSphereRenderable =
//                                    ShapeFactory.makeSphere(0.1f, new Vector3(0.0f, 0.15f, 0.0f), material); });

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.GREEN))
                .thenAccept(
                        material -> {
                            greenCubeRenderable =
                                    ShapeFactory.makeCube(new Vector3(0.1f, 0.1f, 0.1f), new Vector3(0, 0.15f, 0), material); });

//        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.BLUE))
//                .thenAccept(
//                        material -> {
//                            blueCylinderRenderable =
//                                    ShapeFactory.makeCylinder(0.05f, 0.1f, new Vector3(0, 0.15f, 0), material); });

        ModelRenderable.builder()
                .setSource(this, Uri.parse("tree01.sfb"))
                .build()
                .thenAccept(renderable -> treeRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Log.e(TAG, "Unable to load Renderable.", throwable);
                            return null;
                });

        arFragment.setOnSingleTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (greenCubeRenderable == null) {
                        return;
                    }

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    // Create the transformable andy and add it to the anchor.
                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                    andy.setParent(anchorNode);
                    andy.setRenderable(greenCubeRenderable);
                    andy.getScaleController().setMaxScale(0.5f);
                    andy.getScaleController().setMinScale(0.499f);
                    // andy.getTranslationController().setAllowedPlaneTypes(EnumSet.complementOf(andy.getTranslationController().getAllowedPlaneTypes()));
                    // andy.setCollisionShape(new Sphere(0.05f));

                    if (targetAnchor == null) {
                        targetAnchor = anchorNode;
                        targetNode = andy;
                    }

                    Log.i(TAG, "plane center pose: " + plane.getCenterPose());
                });

//        arFragment.setOnLongPressArPlaneListener(
//                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
//                    if (treeRenderable == null) {
//                        return;
//                    }
//
//                    Log.i(TAG, "motionEvent: " + motionEvent.toString());
//
//                    // Create the Anchor.
//                    Anchor anchor = hitResult.createAnchor();
//                    AnchorNode anchorNode = new AnchorNode(anchor);
//                    anchorNode.setParent(arFragment.getArSceneView().getScene());
//
//                    // Create the transformable andy and add it to the anchor.
//                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
//                    andy.setParent(anchorNode);
//                    andy.setRenderable(treeRenderable);
//                    andy.getScaleController().setMaxScale(1f);
//                    andy.getScaleController().setMinScale(0.9f);
//                    andy.getTranslationController().setAllowedPlaneTypes(EnumSet.complementOf(andy.getTranslationController().getAllowedPlaneTypes()));
//                    Log.i(TAG, "plane: " + plane.getPolygon());
//                }
//        );

        arFragment.setOnFlingListener(
                (float velocityX, float velocityY) -> {
                    translation[0] = translation[0] + velocityX/100000;
                    translation[2] = translation[2] + velocityY/100000;
                    //rotation[1] = rotation[1] + velocityX/1000000;
                    //rotation[3] = rotation[3] + velocityY/1000000;
                    // Log.i(TAG, "fling velocity: [" + velocityX + ", " + velocityY + "]" );
                }
        );

        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onFrame);
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

    public static void placeRenderableInRandomPosition(Plane plane, ModelRenderable renderable, MyArFragment arFragment) {
        float maxX = plane.getExtentX()*1.2f;
        float maxZ = plane.getExtentZ()*1.2f;

        float randomX = (random.nextFloat()*maxX) - plane.getExtentX()*0.6f;
        float randomZ = (random.nextFloat()*maxZ) - plane.getExtentZ()*0.6f;

        Pose pose = plane.getCenterPose();
        float[] translation = pose.getTranslation();
        float[] rotation = pose.getRotationQuaternion();

        translation[0] += randomX;
        translation[2] += randomZ;
        pose = new Pose(translation, rotation);

        Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(pose);

        Node node = new Node();
        node.setRenderable(renderable);
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.addChild(node);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
    }

    private void onFrame(FrameTime frameTime) {
        // Keep track of the first valid plane detected, update it
        // if the plane is lost or subsumed.

        if (targetAnchor != null) {
            Anchor previousAnchor = targetAnchor.getAnchor();

            angle += 5;

            float[] newTranslation = new float[3];
            newTranslation[0] = previousAnchor.getPose().tx() + translation[0];
            newTranslation[1] = 0;// previousAnchor.getPose().ty() + translation[1];
            newTranslation[2] = previousAnchor.getPose().tz() + translation[2];
            Log.i(TAG, "translation: " + Arrays.toString(newTranslation));

            float[] newRotation = new float[4];
            newRotation[0] = (float) Math.cos(angle*Math.PI/180);
            newRotation[1] = 0.57735f * (float) Math.sin(angle*Math.PI/180); // previousAnchor.getPose().qy() + rotation[1];
            newRotation[2] = 0.57735f * (float) Math.sin(angle*Math.PI/180); // previousAnchor.getPose().qz() + rotation[2];
            newRotation[3] = 0.57735f * (float) Math.sin(angle*Math.PI/180); // previousAnchor.getPose().qw() + rotation[3];

            if (angle >= 180) { // && rotation[i] < 0) {
                angle = -180;
            }
            if (angle < -180) { // && rotation[i] > 0) {
                angle = 180;
            }

            Log.i(TAG, "rotation: " + Arrays.toString(newRotation));

            targetAnchor.setAnchor(arFragment.getArSceneView().getSession().createAnchor(
                    new Pose(newTranslation, newRotation)
            ));

            previousAnchor.detach();
            // Log.i(TAG, "pose after:  " + targetAnchor.getAnchor().getPose());

            Log.i(TAG, "collisionShape: " + targetNode.getCollisionShape());

            for (int i = 0; i < 3; i++) {
                translation[i] = translation[i]*0.9f;
                if (Math.abs(translation[i]) < 0.01) {
                    translation[i] = 0;
                }
            }
            for (int i = 0; i < 4; i++) {
                rotation[i] = rotation[i]*0.9f;
                if (Math.abs(rotation[i]) < 0.01) {
                    rotation[i] = 0;
                }
            }
            
            if (!arFragment.getArSceneView().getScene().overlapTestAll(targetNode).isEmpty()) {
                for (Node collidingNode: arFragment.getArSceneView().getScene().overlapTestAll(targetNode)) {
                    Log.i(TAG, "colliding node: " + collidingNode.toString());
                }
            }
        }

        if (arFragment.getArSceneView().getSession().getAllAnchors().size() >= 5 && !targetsPlaced) {
            for (Plane plane: arFragment.getArSceneView().getSession().getAllTrackables(Plane.class)) {
                // Log.i(TAG, "plane polygon: " + plane.getPolygon());
                int numOfRenderablesToPlace = random.nextInt(5);
                for (int i = 0; i < numOfRenderablesToPlace; i++) {
                    placeRenderableInRandomPosition(plane, treeRenderable, arFragment);
                }
            }
            targetsPlaced = true;
        }
    }
}
