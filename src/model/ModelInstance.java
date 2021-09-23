package model;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.pooling.*;
import model.Model.*;
import model.attribute.*;
import model.part.*;

/**
 * Defines instances of the {@link Model} asset that are seen in the world. Model instances copy their model's nodes and
 * materials, and has its own transformation matrix.
 */
public class ModelInstance{
    /** The {@link Model} that this instance is bound to. */
    public final Model model;
    /** The model instance's transformation matrix. */
    public final Mat3D trns = new Mat3D();

    /** All the copied {@link Node}s that this instance contains, mapped with their IDs. */
    public final ObjectMap<String, Node> nodes = new ObjectMap<>(2);
    /** All the copied {@link Material}s that this instance contains, mapped with their IDs. */
    public final ObjectMap<String, Material> materials = new ObjectMap<>(2);

    /** Creates a model instance with the specified {@link Model}. */
    public ModelInstance(Model model){
        this.model = model;
        for(var entry : model.nodes) nodes.put(entry.key, entry.value.copy());
        for(var entry : model.materials) materials.put(entry.key, entry.value.copy());
        for(var node : nodes.values()) remapNodes(node);
    }

    private void remapNodes(Node parent){
        for(var node : parent.children.values()){
            for(var part : node.parts){
                part.material = material(part.material.id);
            }

            if(!node.children.isEmpty()) remapNodes(node);
        }
    }

    /** Calculates the transforms of this model's {@link Node}s. */
    public void calcTrns(){
        for(var node : nodes.values()) node.calcTrns(trns);
    }

    /**
     * Retrieves all necessary {@link ModelView}s to be drawn and adds them to the given array.
     * @param array The array to be filled with the pooled {@link ModelView}s.
     */
    public void views(Pool<ModelView> pool, Seq<ModelView> array){
        for(var node : nodes.values()) node.views(pool, array);
    }

    /** @return The {@link Material} with the specified ID, or null if there are none. */
    public Material material(String id){
        return materials.get(id);
    }

    /** @return The recursively searched {@link Node} with the specified ID, or null if there are none. */
    public Node node(Node parent, String id){
        var set = parent == null ? nodes : parent.children;
        if(set.containsKey(id)) return set.get(id);

        for(var node : set.values()){
            var res = node(node, id);
            if(res != null) return res;
        }

        return null;
    }
}
