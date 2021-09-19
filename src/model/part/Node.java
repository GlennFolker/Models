package model.part;

import arc.math.geom.*;
import arc.struct.*;
import model.*;
import model.Model.*;
import model.attribute.*;
import model.part.MeshPair.*;

/**
 * A node represents an object within a {@link Model}. Nodes have IDs, translations, rotations, and scales. They act like
 * a tree; each optionally has a parent and children.
 */
public class Node{
    /** The node ID. */
    public String id;

    /** The translation of this node, typically used for positions. */
    public final Vec3 translation = new Vec3();
    /** The rotation of this node. */
    public final Quat rotation = new Quat();
    /** The scaling of this node. */
    public final Vec3 scaling = new Vec3(1f, 1f, 1f);

    /** The local transformation of this node. */
    public final Mat3D localTrns = new Mat3D();
    /** The transformation of this node relative to its parent. */
    public final Mat3D worldTrns = new Mat3D();

    /** All the {@link NodePart}s that this node contains. */
    public final Seq<NodePart> parts = new Seq<>();

    /**
     * The specific parts of a {@link Node}; the smallest components of a {@link Model}. The node part contains a
     * {@link MeshPart}, a {@link Material}, and specific UV mappings, and participates in rendering of a {@link ModelView}.
     */
    public class NodePart{
        /** The {@link MeshPart} that is bound to this node part. */
        public MeshPart part;
        /** The {@link Material} that is bound to this node part. */
        public Material material;
    }
}
