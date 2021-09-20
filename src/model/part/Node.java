package model.part;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.pooling.*;
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

    /** This node's parent, may be null if has none. */
    public Node parent;
    /** This node's children nodes. */
    public final Seq<Node> children = new Seq<>();
    /** All the {@link NodePart}s that this node contains. */
    public final Seq<NodePart> parts = new Seq<>();

    public Node(){}

    public Node(Node from){
        id = from.id;
        parent = from.parent;

        translation.set(from.translation);
        rotation.set(from.rotation);
        scaling.set(from.scaling);
        children.set(from.children.map(Node::copy));
        parts.set(from.parts.map(NodePart::new));

        calcTrns();
    }

    public Node copy(){
        return new Node(this);
    }

    public void calcTrns(){
        calcTrns(parent != null ? parent.worldTrns : null);
    }

    public void calcTrns(Mat3D trns){
        localTrns.set(translation, rotation, scaling);
        if(trns != null) worldTrns.set(trns).mul(localTrns);

        children.each(Node::calcTrns);
    }

    public void views(Pool<ModelView> pool, Seq<ModelView> array){
        parts.each(part -> array.add(part.view(pool)));
        children.each(child -> child.views(pool, array));
    }

    /**
     * The specific parts of a {@link Node}; the smallest components of a {@link Model}. The node part contains a
     * {@link MeshPart}, a {@link Material}, and specific UV mappings, and participates in rendering of a {@link ModelView}.
     */
    public class NodePart{
        /** The {@link MeshPart} that is bound to this node part. */
        public MeshPart mesh;
        /** The {@link Material} that is bound to this node part. */
        public Material material;

        public NodePart(){}

        public NodePart(NodePart from){
            mesh = from.mesh;
            material = from.material;
        }

        /** @return The {@link Node} this part is bound to. */
        public Node node(){
            return Node.this;
        }

        /** @return A {@link ModelView} that matches this node part's properties. */
        public ModelView view(Pool<ModelView> pool){
            var view = pool.obtain();
            return view.set(this);
        }
    }
}
