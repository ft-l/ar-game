package com.example.argame;

import android.content.Context;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.TransformableNode;

public class BoundsMarker {
    private static final String TAG = BoundsMarker.class.getSimpleName();

    private BoundsMarker parentMarker;
    private TransformableNode marker;
    private Node xLine;
    private Node zLine;

    public BoundsMarker(HitResult hitResult, MyArFragment parentFragment, Context context) {
        Anchor anchor = hitResult.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(parentFragment.getArSceneView().getScene());

        // Create the marker and add it to the anchor.
        marker = new TransformableNode(parentFragment.getTransformationSystem());
        marker.getScaleController().setSensitivity(0);
        marker.getRotationController().setRotationRateDegrees(0);
        marker.setWorldRotation(new Quaternion());
        marker.setParent(anchorNode);

        xLine = new Node();
        xLine.setParent(marker);
        xLine.setLocalPosition(new Vector3(0,0,0));
        xLine.setLocalRotation(new Quaternion());

        zLine = new Node();
        zLine.setParent(marker);
        zLine.setLocalPosition(new Vector3(0,0,0));
        zLine.setLocalRotation(new Quaternion(0, (float) Math.sqrt(0.5), 0, (float) Math.sqrt(0.5)));

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.BLUE))
                .thenAccept(
                        material -> {
                            marker.setRenderable(ShapeFactory.makeCylinder(0.03f, 0.05f, new Vector3(0f, 0f, 0), material));
                        }
                );

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.BLACK))
                .thenAccept(
                        material -> {
                            ModelRenderable alignmentLinesRenderable =
                                    ShapeFactory.makeCube(new Vector3(10f,0.001f,0.001f), new Vector3(0,0,0), material);
                            xLine.setRenderable(alignmentLinesRenderable);
                            zLine.setRenderable(alignmentLinesRenderable);
                        }
                );
    }

    public BoundsMarker(HitResult hitResult, MyArFragment parentFragment, Context context, BoundsMarker parentMarker) {
        Anchor anchor = hitResult.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(parentFragment.getArSceneView().getScene());

        // Create the marker and add it to the anchor.
        marker = new TransformableNode(parentFragment.getTransformationSystem());
        marker.getScaleController().setSensitivity(0);
        marker.getRotationController().setRotationRateDegrees(0);
        marker.setWorldRotation(new Quaternion());
        marker.setParent(anchorNode);

        xLine = new Node();
        xLine.setParent(marker);
        xLine.setLocalPosition(new Vector3(0,0,0));
        xLine.setLocalRotation(new Quaternion());

        zLine = new Node();
        zLine.setParent(marker);
        zLine.setLocalPosition(new Vector3(0,0,0));
        zLine.setLocalRotation(new Quaternion(0, (float) Math.sqrt(0.5), 0, (float) Math.sqrt(0.5)));

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.BLUE))
                .thenAccept(
                        material -> {
                            marker.setRenderable(ShapeFactory.makeCylinder(0.03f, 0.05f, new Vector3(0f, 0f, 0), material));
                        }
                );

        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.BLACK))
                .thenAccept(
                        material -> {
                            ModelRenderable alignmentLinesRenderable =
                                    ShapeFactory.makeCube(new Vector3(10f,0.001f,0.001f), new Vector3(0,0,0), material);
                            xLine.setRenderable(alignmentLinesRenderable);
                            zLine.setRenderable(alignmentLinesRenderable);
                        }
                );
        this.parentMarker = parentMarker;
    }

    /**
        Returns an array of the [x,z] position of this marker
        [0] = marker x
        [1] = marker y
        [2] = marker z
     */
    public float[] getPosition() {
        return new float[]{marker.getWorldPosition().x, marker.getWorldPosition().y, marker.getWorldPosition().z};
    }

    public void resetRotation() {
        if (parentMarker != null) {
            marker.setWorldRotation(parentMarker.marker.getWorldRotation());
        }
    }
}
