package model;

import arc.math.*;
import arc.struct.*;
import model.part.*;
import model.part.Anim.*;

/** A {@link ModelInstance} animation controller. */
public class Anims{
    /** The target model instance to be animated. */
    public ModelInstance model;

    /** Constructs an empty unusable animation controller. {@link #model} must be set later on. */
    public Anims(){}

    /** Constructs an animation controller targeting the specified {@link ModelInstance}. */
    public Anims(ModelInstance model){
        this.model = model;
    }

    /** Begins an animation controlling. Sets all {@link Node#animated} to false and calculates initial transforms. */
    public void begin(){
        model.nodes(node -> node.each(n -> n.animated = false));
        model.calcTrns();
    }

    /** Ends an animation controlling. Recalculates {@link Node} transforms and sets all {@link Node#animated} to false. */
    public void end(){
        model.calcTrns();
        model.nodes(node -> node.each(n -> n.animated = false));
    }

    /** Applies an animation with the specified time frame. Sets related {@link Node}'s {@link Node#animated} to true. */
    public void animate(String id, float time){
        animate(model.anim(id), time);
    }

    /** Applies an animation with the specified time frame. Sets related {@link Node}'s {@link Node#animated} to true. */
    public void animate(Anim anim, float time){
        for(var a : anim.anims){
            var node = a.node;
            node.animated = true;

            var trns = frame(a.trns, time);
            if(trns != null) node.localTrns.translate(trns.value);

            var rot = frame(a.rot, time);
            if(rot != null) node.localTrns.rotate(rot.value);

            var scl = frame(a.scl, time);
            if(scl != null) node.localTrns.scale(scl.value.x, scl.value.y, scl.value.z);
        }
    }

    /** {@link #animate(String, float)}, but time ranges from [0..1]. */
    public void animateFrac(String id, float time){
        animateFrac(model.anim(id), time);
    }

    /** {@link #animate(Anim, float)}, but time ranges from [0..1]. */
    public void animateFrac(Anim anim, float time){
        animate(anim, Mathf.clamp(time) * anim.duration);
    }

    private static <T> Keyframe<T> frame(Seq<Keyframe<T>> frames, float time){
        if(frames.isEmpty()) return null;

        int last = frames.size - 1;
        if(time < frames.get(0).time || time > frames.get(last).time){
            return null;
        }

        int min = 0,
            max = last;

        while(min < max){
            int i = (min + max) / 2;
            if(time > frames.get(i + 1).time){
                min = i + 1;
            }else if(time < frames.get(i).time){
                max = i - 1;
            }else{
                return frames.get(i);
            }
        }

        return frames.get(min);
    }
}
