package com.example.argame.gamepieces;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

public class PieceCreationUtils {

    public static Node createPiece(PiecePart[] parts) {
        Node parentNode = new Node();
        for (PiecePart part: parts) {
            Node partNode = new Node();
            partNode.setRenderable(part.renderable);
            partNode.setParent(parentNode);
            partNode.setLocalPosition(part.relativePosition);
            partNode.setLocalRotation(part.relativeRotation);
        }
        return parentNode;
    }

    public class PiecePart {
        private ModelRenderable renderable;
        private Vector3 relativePosition;
        private Quaternion relativeRotation;

        public PiecePart(ModelRenderable renderable, Vector3 relativePosition, Quaternion relativeRotation) {
            this.renderable = renderable;
            this.relativePosition = relativePosition;
            this.relativeRotation = relativeRotation;
        }
    }
}
