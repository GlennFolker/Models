package model.part;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.pooling.*;
import model.*;
import model.Model.*;
import model.attribute.*;

/**
 * A node represents an object within a {@link Model}. Nodes have IDs, translations, rotations, and scales. They act like
 * a tree; each optionally has a parent and children.
 */
public class Node{
    /** The node ID. */
    public String id;

    /** The translation of this node, typically used for positions. Unaffected by animations. */
    public final Vec3 translation = new Vec3();
    /** The rotation of this node. Unaffected by animations. */
    public final Quat rotation = new Quat();
    /** The scaling of this node. Unaffected by animations. */
    public final Vec3 scaling = new Vec3(1f, 1f, 1f);

    /** The local transformation of this node. */
    public final Mat3D localTrns = new Mat3D();
    /** The transformation of this node relative to its parent. */
    public final Mat3D worldTrns = new Mat3D();

    /** This node's parent, may be null if has none. */
    public Node parent;
    /** This node's children nodes. */
    public final ObjectMap<String, Node> children = new ObjectMap<>(6);
    /** All the {@link NodePart}s that this node contains. */
    public final Seq<NodePart> parts = new Seq<>(2);

    public Node(){}

    public Node(Node from){
        id = from.id;
        parent = from.parent;

        translation.set(from.translation);
        rotation.set(from.rotation);
        scaling.set(from.scaling);
        parts.set(from.parts.map(NodePart::new));
        for(var child : from.children.entries()) children.put(child.key, child.value.copy());

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

        for(var child : children.values()) child.calcTrns();
    }

    public void views(Pool<ModelView> pool, Seq<ModelView> array){
        for(var part : parts) array.add(part.view(pool));
        for(var child : children.values()) child.views(pool, array);
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
