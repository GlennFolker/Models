package model.part;

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
