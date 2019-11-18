package com.example.argame;

import android.content.Context;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;

import java.util.ArrayList;

public class PlayerBall {
    private static final String TAG = PlayerBall.class.getSimpleName();
    private static final float ROTATION_ADJUSTMENT = 0.4f;

    private MyArFragment parentFragment;

    private AnchorNode anchorNode;
    private Node ball;
    private Renderable ballModel;

    /**
     * bounds[0] = x min
     * bounds[1] = x max
     * bounds[2] = z min
     * bounds[3] = z max
     */
    private float[] bounds;

    private int rotations = 0;

    private float radius;

    private Vector3 velocity = new Vector3();

    public PlayerBall(HitResult hitResult, MyArFragment parentFragment, Context context, float radius, float[] bounds) {

        this.bounds = bounds;
        this.parentFragment = parentFragment;
        this.radius = radius;

        // Create the Anchor.
        Anchor anchor = hitResult.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(parentFragment.getArSceneView().getScene());

        // Create the ball and add it to the anchor.
        Node playerBall = new Node();
        playerBall.setParent(anchorNode);

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.RED))
            .thenAccept(
                material -> {
                    ballModel = ShapeFactory.makeSphere(radius, new Vector3(0.0f, 0.0f, 0.0f), material);
                    playerBall.setRenderable(ballModel);
                }
            );

        this.anchorNode = anchorNode;
        ball = playerBall;
    }

    public Node getBall() {
        return ball;
    }

    public AnchorNode getAnchorNode() {
        return anchorNode;
    }

    public void setVelocity(Vector3 velocity) {
        this.velocity = velocity;
    }

    public Vector3 getVelocity() {
        return velocity;
    }

    public void update() {
        Anchor previousAnchor = anchorNode.getAnchor();

        Frame frame = null;
        try {
            frame = parentFragment.getArSceneView().getSession().update();
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        // ball.setCollisionShape(new Sphere(radius/(velocity.length()*0.5f)));

        ArrayList<Node> collisions = parentFragment.getArSceneView().getScene().overlapTestAll(ball);

        for (Node node : ball.getChildren()) {
            collisions.addAll(parentFragment.getArSceneView().getScene().overlapTestAll(node));
        }

        for (Node node: collisions) {
            if (!node.getParent().equals(ball) && !node.equals(ball)) {
                Vector3 relativePosition = Quaternion.rotateVector(ball.getWorldRotation().inverted(),
                                                        Vector3.subtract(node.getWorldPosition(),ball.getWorldPosition()));
                Quaternion relativeRotation = Quaternion.multiply(ball.getWorldRotation().inverted(),
                                                        node.getWorldRotation());
                node.setParent(ball);
                node.setLocalPosition(relativePosition);
                node.setLocalRotation(relativeRotation);
            }

//            Log.i(TAG, "this node is overlapping: " + node);
//            Log.i(TAG, "ball world position: " + ball.getWorldPosition().toString());
//            Log.i(TAG, "coll world position: " + node.getWorldPosition().toString());
        }

        float[] newPosition = getTranslatedPosition(previousAnchor, velocity);

        if (frame != null && frame.getCamera().getTrackingState() == TrackingState.TRACKING &&
                newPosition[0] > bounds[0] &&
                newPosition[0] < bounds[1] &&
                newPosition[2] > bounds[2] &&
                newPosition[2] < bounds[3]) {
            anchorNode.setAnchor(parentFragment.getArSceneView().getSession().createAnchor(
                    new Pose(getTranslatedPosition(previousAnchor, velocity), getBallRotatedRotation(previousAnchor, velocity, radius))
            ));
            previousAnchor.detach();
        }

        velocity.x = velocity.x*0.95f;
        velocity.y = velocity.y*0.95f;
        if (Math.abs(velocity.x) < 0.00001) {
            velocity.x = 0;
        }
        if (Math.abs(velocity.y) < 0.00001) {
            velocity.y = 0;
        }
    }

    private static float[] getTranslatedPosition(Anchor anchor, Vector3 motion) {
        float[] newTranslation = new float[3];
        newTranslation[0] = anchor.getPose().tx() + motion.x;
        newTranslation[1] = anchor.getPose().ty();
        newTranslation[2] = anchor.getPose().tz() + motion.y;
        return newTranslation;
    }

    private static float[] getBallRotatedRotation(Anchor anchor, Vector3 motion, float radius) {
        Quaternion currentRotation = new Quaternion(anchor.getPose().qx(), anchor.getPose().qy(), anchor.getPose().qz(), anchor.getPose().qw());

        Quaternion newRotation = Quaternion.multiply(getRotationQuaternionForRollingInDirection(motion, radius), currentRotation);

        return new float[]{newRotation.x, newRotation.y, newRotation.z, newRotation.w};
    }

    private static Quaternion getRotationQuaternionForRollingInDirection(Vector3 motion, float radius) {
        if (motion.x != 0 && motion.y != 0) {
            double angleOfMotion = Math.atan2(-motion.y,-motion.x);

            double magnitude = Math.sqrt((motion.y*motion.y)+(motion.x*motion.x)) * ROTATION_ADJUSTMENT / radius;

//            Log.i(TAG, "x: " + -motion.x + " y: " + -motion.y);
//            Log.i(TAG, "angle of motion: " + (-angleOfMotion + (3*Math.PI/2)) * 180/Math.PI);
//            Log.i(TAG, "magnitude: " + magnitude);

            return new Quaternion((float) (Math.sin(magnitude/2)*Math.cos(-angleOfMotion + (3*Math.PI/2))),
                    0,
                    (float) (Math.sin(magnitude/2)*-Math.sin(-angleOfMotion + (3*Math.PI/2))),
                    (float) (Math.cos(magnitude/2))).normalized();
        } else {
            return new Quaternion(0,0,0,1);
        }
    }

//    public void placeCube() {
//        Quaternion rotation = new Quaternion(-0.70710678f,0,0,0.70710678f).normalized();
//        Vector3 point = new Vector3(0.25f,0,0.25f);
//        for (int i = 0; i < rotations; i++) {
//            point = Quaternion.rotateVector(rotation, point);
//        }
//
//        Node stuckCube = new Node();
//        stuckCube.setParent(ball);
//        stuckCube.setLocalPosition(point);
//        stuckCube.setRenderable(cubeModel);
//
//        rotations++;
//    }
}
