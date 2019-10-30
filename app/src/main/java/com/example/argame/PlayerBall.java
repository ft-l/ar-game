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

public class PlayerBall {
    private static final String TAG = PlayerBall.class.getSimpleName();
    private static final float ROTATION_ADJUSTMENT = 50f;

    private MyArFragment parentFragment;

    private AnchorNode anchorNode;
    private Node ball;
    private Renderable ballModel;

    private Renderable cubeModel;
    private int rotations = 0;

    private float radius;

    private Vector3 velocity = new Vector3();

    public PlayerBall(HitResult hitResult, MyArFragment parentFragment, Context context, float radius) {

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

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.GREEN))
            .thenAccept(
                material -> cubeModel = ShapeFactory.makeCube(new Vector3(0.1f,0.1f,0.1f), new Vector3(0,0,0), material)
            );

//        Node stuckCube = new Node();
//        stuckCube.setParent(playerBall);
//        stuckCube.setLocalPosition(new Vector3(0.3f,0,0));
//        stuckCube.setRenderable(outsideRenderable);

        // andy.getTranslationController().setAllowedPlaneTypes(EnumSet.complementOf(andy.getTranslationController().getAllowedPlaneTypes()));
        // andy.setCollisionShape(new Sphere(0.05f));

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

        Log.i(TAG, "update called");
        Log.i(TAG, "previousAnchor pose: " + previousAnchor.getPose());

        for (Node node: parentFragment.getArSceneView().getScene().overlapTestAll(ball)) {
            if (!node.getParent().equals(ball)) {
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

        if (frame != null && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            anchorNode.setAnchor(parentFragment.getArSceneView().getSession().createAnchor(
                    new Pose(getTranslatedPosition(previousAnchor, velocity), getBallRotatedRotation(previousAnchor, velocity, radius))
            ));
        }

        previousAnchor.detach();

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

            double magnitude = Math.sqrt((motion.y*motion.y)+(motion.x*motion.x)) * radius * ROTATION_ADJUSTMENT;

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

    private static Quaternion getRotatedQuaternionByEulerAngles(Quaternion quaternion, float x, float y, float z) {

        quaternion.normalize();

        float[] eulerAngles = quaternionToEulerAngles(quaternion);

//        eulerAngles[0] += (y * ROTATION_ADJUSTMENT);
        eulerAngles[1] += (x * ROTATION_ADJUSTMENT);
//        eulerAngles[2] += (z * ROTATION_ADJUSTMENT);

        Log.i(TAG, "heading: " + eulerAngles[0]);
        Log.i(TAG, "attitude: " + eulerAngles[1]);
        Log.i(TAG, "bank: " + eulerAngles[2]);

        return eulerAnglesToQuaternion(eulerAngles);
    }

    private static float[] quaternionToEulerAngles(Quaternion quaternion) {
        float[] angles = new float[3];
        double sqw = quaternion.w*quaternion.w;
        double sqx = quaternion.x*quaternion.x;
        double sqy = quaternion.y*quaternion.y;
        double sqz = quaternion.z*quaternion.z;
        double unit = sqx + sqy + sqz + sqw; // if normalised is one, otherwise is correction factor
        double test = quaternion.x*quaternion.y + quaternion.z*quaternion.w;
        Log.i(TAG, "xy+wz: " + test);
        if (test > 0.49999999999999*unit) { // singularity at north pole
            angles[0] = (float) (2 * Math.atan2(quaternion.x,quaternion.w));    // heading
            angles[1] = (float) Math.PI/2;                                      // attitude
            angles[2] = 0f;                                                     // bank
            return angles;
        }
        if (test < -0.49999999999999*unit) { // singularity at south pole
            angles[0] = (float) (-2 * Math.atan2(quaternion.x,quaternion.w));   // heading
            angles[1] = (float) -Math.PI/2;                                     // attitude
            angles[2] = 0f;                                                     // bank
            return angles;
        }
        angles[0] = (float) Math.atan2(2*quaternion.y*quaternion.w-2*quaternion.x*quaternion.z , sqx - sqy - sqz + sqw);    // heading
        angles[1] = (float) Math.asin(2*test/unit);                                                                         // attitude
        angles[2] = (float) Math.atan2(2*quaternion.x*quaternion.w-2*quaternion.y*quaternion.z , -sqx + sqy - sqz + sqw);   // bank
        return angles;
    }

    private static Quaternion eulerAnglesToQuaternion(float[] angles) {
        // Assuming the angles are in radians.
        double c1 = Math.cos(angles[0]/2);
        double s1 = Math.sin(angles[0]/2);
        double c2 = Math.cos(angles[1]/2);
        double s2 = Math.sin(angles[1]/2);
        double c3 = Math.cos(angles[2]/2);
        double s3 = Math.sin(angles[2]/2);
        double c1c2 = c1*c2;
        double s1s2 = s1*s2;
        return new Quaternion((float)(c1c2*s3 + s1s2*c3),
                              (float)(s1*c2*c3 + c1*s2*s3),
                              (float)(c1*s2*c3 - s1*c2*s3),
                              (float)(c1c2*c3 - s1s2*s3));
    }

    public void placeCube() {
        Quaternion rotation = new Quaternion(-0.70710678f,0,0,0.70710678f).normalized();
        Vector3 point = new Vector3(0.25f,0,0.25f);
        for (int i = 0; i < rotations; i++) {
            point = Quaternion.rotateVector(rotation, point);
        }

        Node stuckCube = new Node();
        stuckCube.setParent(ball);
        stuckCube.setLocalPosition(point);
        stuckCube.setRenderable(cubeModel);

        rotations++;
    }
}
