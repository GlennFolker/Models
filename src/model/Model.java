package model;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.Pool.*;
import model.attribute.*;
import model.part.*;
import model.part.MeshPair.*;

/** A model is a complete set of meshes with specified indices offset and length, model nodes tree, and materials. */
public class Model implements Disposable{
    /** The model ID. */
    public String id = "";

    /** All the {@link MeshPair}s that this model contains. */
    public final Seq<MeshPair> meshes = new Seq<>();
    /** All the {@link Node}s that this model contains. */
    public final Seq<Node> nodes = new Seq<>();
    /** All the {@link Material}s that this model contains. */
    public final Seq<Material> materials = new Seq<>();

    @Override
    public void dispose(){
        meshes.each(Disposable::dispose);

        meshes.clear();
        nodes.clear();
        materials.clear();
    }

    /** A packed renderable view of a {@link Model} used in {@link ModelShader} to specify renderings. */
    public static class ModelView implements Poolable{
        /** The transform matrix of this view. */
        public final Mat3D trns = new Mat3D();
        /** The {@link MeshPart} of this model to be rendered by the shader. */
        public MeshPart part;
        /** The {@link Material} of this view. */
        public Material material;

        @Override
        public void reset(){
            trns.idt();
            part = null;
            material = null;
        }
    }
}
