package model.part;

import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import model.*;

/** Defines an animation of a {@link Model}. */
public class Anim{
    /** The ID of this animation. */
    public String id = "";

    /** The total duration of this animation. */
    public float duration;
    /** All the {@link NodeAnim}s that this animation contains. */
    public final Seq<NodeAnim> anims = new Seq<>(2);

    /** Constructs an empty animation. */
    public Anim(){}

    /**
     * Constructs an animation based on other animation.
     * @param nodes {@link Node} mapper, to get the nodes that each {@link NodeAnim} will use.
     */
    public Anim(Func<Node, Node> nodes, Anim other){
        id = other.id;
        duration = other.duration;

        anims.set(other.anims.map(a -> a.copy(nodes.get(a.node))));
    }

    /**
     * @param nodes {@link Node} mapper, to get the nodes that each {@link NodeAnim} will use.
     * @return An exact copy of this animation.
     */
    public Anim copy(Func<Node, Node> nodes){
        return new Anim(nodes, this);
    }

    /** Defines a set of keyframes that manipulate the bound {@link Node}. */
    public static class NodeAnim{
        /** The {@link Node} that this animation is bound to. */
        public final Node node;

        /** Keyframes set manipulating the {@link #node}'s translation. */
        public final Seq<Keyframe<Vec3>> trns = new Seq<>();
        /** Keyframes set manipulating the {@link #node}'s rotation. */
        public final Seq<Keyframe<Quat>> rot = new Seq<>();
        /** Keyframes set manipulating the {@link #node}'s scaling. */
        public final Seq<Keyframe<Vec3>> scl = new Seq<>();

        /** Constructs an empty node animation. */
        public NodeAnim(Node node){
            this.node = node;
        }

        /** Constructs a node animation based on other animation. Note that each {@link Keyframe}s don't get copied. */
        public NodeAnim(Node node, NodeAnim other){
            this.node = node;
            trns.set(other.trns);
            rot.set(other.rot);
            scl.set(other.scl);
        }

        /** @return An exact copy of this node animation. */
        public NodeAnim copy(Node node){
            return new NodeAnim(node, this);
        }

        /** Sorts all keyframes. */
        public NodeAnim sort(){
            trns.sort();
            rot.sort();
            scl.sort();
            return this;
        }
    }

    /** Defines a timed keyframe containing values such as translation, rotation, or scaling. */
    public static class Keyframe<T> implements Comparable<Keyframe<T>>{
        /** This keyframe's key time. */
        public float time;
        /** This keyframe's value. */
        public T value;

        /** Constructs an empty keyframe. */
        public Keyframe(){}

        /** Constructs a keyframe with given time and value. */
        public Keyframe(float time, T value){
            this.time = time;
            this.value = value;
        }

        @Override
        public int compareTo(Keyframe<T> other){
            return Float.compare(time, other.time);
        }
    }
}
