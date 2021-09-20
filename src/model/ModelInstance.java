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

    /** All the copied {@link Node}s that this instance contains. */
    public final Seq<Node> nodes;
    /** All the copied {@link Material}s that this instance contains. */
    public final Seq<Material> materials;

    /** Creates a model instance with the specified {@link Model}. */
    public ModelInstance(Model model){
        this.model = model;
        nodes = model.nodes.map(Node::copy);
        materials = model.materials.map(Material::copy);

        nodes.each(this::remapNodes);
    }

    private void remapNodes(Node parent){
        for(var node : parent.children){
            for(var part : node.parts){
                part.material = material(part.material.id);
            }

            if(!node.children.isEmpty()) remapNodes(node);
        }
    }

    /** Calculates the transforms of this model's {@link Node}s. */
    public void calcTrns(){
        nodes.each(node -> node.calcTrns(trns));
    }

    /**
     * Retrieves all necessary {@link ModelView}s to be drawn and adds them to the given array.
     * @param array The array to be filled with the pooled {@link ModelView}s.
     */
    public void views(Pool<ModelView> pool, Seq<ModelView> array){
        nodes.each(node -> node.views(pool, array));
    }

    /** @return The {@link Material} with the specified ID, or null if there are none. */
    public Material material(String id){
        return materials.find(m -> m.id.equals(id));
    }

    /** @return The recursively searched {@link Node} with the specified ID, or null if there are none. */
    public Node node(Node parent, String id){
        var set = parent == null ? nodes : parent.children;
        for(var node : set){ // Flat checks first.
            if(node.id.equals(id)) return node;
        }

        for(var node : set){ // If not found, search it recursively.
            if(!node.children.isEmpty()){
                for(var child : node.children){
                    var res = node(child, id);
                    if(res != null) return res;
                }
            }
        }

        return null;
    }
}
