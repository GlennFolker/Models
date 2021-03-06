package model;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.Pool.*;
import arc.util.serialization.*;
import model.attribute.*;
import model.attribute.Attribute.*;
import model.attribute.Attribute.ColAttr.*;
import model.attribute.Attribute.FAttr.*;
import model.attribute.Attribute.TexAttr.*;
import model.part.*;
import model.part.Anim.*;
import model.part.Node.*;

/** A model is a complete set of meshes with specified indices offset and length, model nodes tree, and materials. */
public class Model implements Disposable{
    /** The model ID. */
    public String id = "";

    /** All the {@link Mesh}s that this model contains. */
    public final Seq<Mesh> meshes = new Seq<>(2);

    /** All the {@link MeshPart}s that this model contains, mapped with their IDs. */
    protected final ObjectMap<String, MeshPart> meshParts = new ObjectMap<>(6);
    /** All the {@link Node}s that this model contains, mapped with their IDs. */
    protected final ObjectMap<String, Node> nodes = new ObjectMap<>(6);
    /** All the {@link Material}s that this model contains, mapped with their IDs. */
    protected final ObjectMap<String, Material> materials = new ObjectMap<>(6);
    /** All the {@link Anim}s that this model contains, mapped with their IDs. */
    protected final ObjectMap<String, Anim> animations = new ObjectMap<>(6);

    /** Loads a model data with the given JSON properties. */
    public void load(JsonValue json){
        dispose();

        id = json.require("id").asString();
        loadMeshes(json.require("meshes"));
        loadMaterials(json.require("materials"));
        loadNodes(json.require("nodes"));
        loadAnimations(json.require("animations"));
    }

    protected void loadMeshes(JsonValue json){
        for(var data = json.child; data != null; data = data.next){
            var attr = Seq.of(VertexAttribute.class);
            int texUnit = 0;

            for(var attrData : data.require("attributes").asStringArray()){
                attrData = attrData.toLowerCase();
                if(attrData.equals("position")){
                    attr.add(VertexAttribute.position3);
                }else if(attrData.equals("normal")){
                    attr.add(VertexAttribute.normal);
                }else if(attrData.startsWith("texcoord")){
                    attr.add(new VertexAttribute(2, Shader.texcoordAttribute + texUnit++));
                }else{
                    throw new IllegalArgumentException("Unsupported vertex attribute: '" + attrData + "'");
                }
            }

            var vertices = data.require("vertices").asFloatSeq();
            var indices = new ShortSeq();

            Seq<MeshPart> parts = new Seq<>(2);
            for(var partData = data.require("parts").child; partData != null; partData = partData.next){
                var part = new MeshPart();
                part.id = partData.require("id").asString();

                var typeData = partData.require("type").asString().toLowerCase();
                part.type = switch(typeData){
                    case "points" -> Gl.points;
                    case "lines" -> Gl.lines;
                    case "lineloop" -> Gl.lineLoop;
                    case "linestrip" -> Gl.lineStrip;
                    case "triangles" -> Gl.triangles;
                    case "trianglestrip" -> Gl.triangleStrip;
                    case "trianglefan" -> Gl.triangleFan;
                    default -> throw new IllegalArgumentException("Invalid primitive type: '" + typeData + "'");
                };

                part.offset = indices.size;
                indices.addAll(partData.require("indices").asShortArray());
                part.count = indices.size - part.offset;

                parts.add(part);
            }

            var mesh = new Mesh(true, vertices.length / attr.sum(a -> a.components), indices.size, attr.toArray());
            mesh.setVertices(vertices);
            mesh.setIndices(indices.toArray());

            meshes.add(mesh);
            for(var part : parts){
                part.mesh = mesh;
                meshPart(part);
            }
        }
    }

    protected void loadMaterials(JsonValue json){
        for(var data = json.child; data != null; data = data.next){
            var mat = new Material();
            mat.id = data.require("id").asString();

            for(var alias : FAlias.all){
                var fdata = data.get(alias.name());
                if(fdata != null) mat.set(new FAttr(alias, fdata.asFloat()));
            }

            for(var alias : ColAlias.all){
                var colData = data.get(alias.name());
                if(colData != null) mat.set(new ColAttr(alias, readCol(new Color(), colData)));
            }

            var textures = data.get("textures");
            if(textures != null && textures.isArray()){
                for(var texData = textures.child; texData != null; texData = texData.next){
                    var usage = texData.require("type").asString();
                    var alias = Structs.find(TexAlias.all, e -> e.name().equals(usage.toLowerCase()));

                    if(alias == null) throw new IllegalArgumentException("Unsupported texture type: '" + usage + "'");

                    mat.set(new TexAttr(alias, texData.require("filename").asString()));
                }
            }

            material(mat);
        }
    }

    protected void loadNodes(JsonValue json){
        for(var data = json.child; data != null; data = data.next){
            var node = new Node();
            node.id = data.require("id").asString();

            var trns = data.get("translation");
            if(trns != null) readVec(node.translation, trns);

            var rot = data.get("rotation");
            if(rot != null) readQuat(node.rotation, rot);

            var scl = data.get("scale");
            if(scl != null) readVec(node.scaling, scl);

            for(var partData = data.require("parts").child; partData != null; partData = partData.next){
                var part = new NodePart(node);
                part.mesh = meshPart(partData.require("meshpartid").asString());
                part.material = material(partData.require("materialid").asString());

                node.parts.add(part);
            }

            node(node);
        }
    }

    protected void loadAnimations(JsonValue json){
        for(var data = json.child; data != null; data = data.next){
            var anim = new Anim();
            anim.id = data.require("id").asString();

            for(var nodeData = data.require("bones").child; nodeData != null; nodeData = nodeData.next){
                var nodeId = nodeData.require("boneId").asString();
                var node = node(nodeId);
                if(node == null) throw new IllegalArgumentException("Node with ID '" + nodeId + "' not found.");

                var nodeAnim = new NodeAnim(node);
                for(var keyData = nodeData.require("keyframes").child; keyData != null; keyData = keyData.next){
                    float time = keyData.getFloat("keytime") / (100f / 3f);
                    anim.duration = Math.max(anim.duration, time);

                    var trns = keyData.get("translation");
                    if(trns != null) nodeAnim.trns.add(new Keyframe<>(time, readVec(new Vec3(), trns)));

                    var rot = keyData.get("rotation");
                    if(rot != null) nodeAnim.rot.add(new Keyframe<>(time, readQuat(new Quat(), rot)));

                    var scl = keyData.get("scale");
                    if(scl != null) nodeAnim.scl.add(new Keyframe<>(time, readVec(new Vec3(), scl)));
                }

                anim.anims.add(nodeAnim.sort());
            }

            if(anim.anims.any()) anim(anim);
        }
    }

    private static Vec3 readVec(Vec3 def, JsonValue data){
        return def.set(
            data.getFloat(0) / 100f,
            data.getFloat(1) / 100f,
            data.getFloat(2) / 100f
        );
    }

    private static Quat readQuat(Quat def, JsonValue data){
        return def.set(
            data.getFloat(0),
            data.getFloat(1),
            data.getFloat(2),
            data.getFloat(3)
        );
    }

    private static Color readCol(Color def, JsonValue data){
        return def.set(
            data.getFloat(0),
            data.getFloat(1),
            data.getFloat(2),
            1f
        );
    }

    @Override
    public void dispose(){
        meshes.each(Disposable::dispose);
        meshes.clear();
        meshParts.clear();
        nodes.clear();
        materials.clear();
    }

    /** Should be called after this model has been loaded. */
    public void init(){
        materials(mat -> mat.each(a -> {
            if(a instanceof TexAttr t) t.remap();
        }));
    }

    /** Adds a {@link MeshPart} to this model. Will throw an exception if a mesh part with the same ID is already contained. */
    public void meshPart(MeshPart part){
        if(meshParts.containsKey(part.id)) throw new IllegalArgumentException("Mesh part with id '" + part.id + "' already exists.");
        meshParts.put(part.id, part);
    }

    /** Adds a {@link Material} to this model. Will throw an exception if a material with the same ID is already contained. */
    public void material(Material mat){
        if(materials.containsKey(mat.id)) throw new IllegalArgumentException("Material with id '" + mat.id + "' already exists.");
        materials.put(mat.id, mat);
    }

    /** Adds a {@link Node} to this model. Will throw an exception if a node with the same ID is already contained. */
    public void node(Node node){
        if(nodes.containsKey(node.id)) throw new IllegalArgumentException("Node with id '" + node.id + "' already exists.");
        nodes.put(node.id, node);
    }

    /** Adds a {@link Anim} to this model. Will throw an exception if an animation with the same ID is already contained. */
    public void anim(Anim anim){
        if(animations.containsKey(anim.id)) throw new IllegalArgumentException("Node with id '" + anim.id + "' already exists.");
        animations.put(anim.id, anim);
    }

    /** @return The {@link MeshPart} with the specified ID, or null if there are none. */
    public MeshPart meshPart(String id){
        return meshParts.get(id);
    }

    /** @return The {@link Material} with the specified ID, or null if there are none. */
    public Material material(String id){
        return materials.get(id);
    }

    /** @return The {@link Anim} with the specified ID, or null if there are none. */
    public Anim anim(String id){
        return animations.get(id);
    }

    /** @return The recursively searched {@link Node} with the specified ID, or null if there are none. */
    public Node node(String id){
        return node(null, id);
    }

    /** @return The recursively searched {@link Node} with the specified ID, or null if there are none. */
    public Node node(Node parent, String id){
        return Node.get(nodes, parent, id);
    }

    /** Applies the consumer to all {@link MeshPart}s this model contains. */
    public void meshParts(Cons<MeshPart> cons){
        for(var part : meshParts.values()) cons.get(part);
    }

    /** Applies the consumer to all {@link Material}s this model contains. */
    public void materials(Cons<Material> cons){
        for(var part : materials.values()) cons.get(part);
    }

    /** Applies the consumer to all {@link Node}s this model contains. */
    public void nodes(Cons<Node> cons){
        for(var part : nodes.values()) cons.get(part);
    }

    /** Applies the consumer to all {@link Anim}s this model contains. */
    public void anims(Cons<Anim> cons){
        for(var part : animations.values()) cons.get(part);
    }

    /** A packed renderable view of a {@link Model} used in {@link ModelShader} to specify renderings. */
    public static class ModelView implements Poolable{
        /** The transform matrix of this view. */
        public final Mat3D trns = new Mat3D();
        /** The {@link MeshPart} of this model to be rendered by the shader. */
        public MeshPart mesh;
        /** The {@link Material} of this view. */
        public Material material;
        /** The {@link Environment} of this view. */
        public Environment env;

        @Override
        public void reset(){
            trns.idt();
            mesh = null;
            material = null;
        }

        /**
         * Sets this view's properties to match the given {@link NodePart}.
         * @return This instance, for convenience.
         */
        public ModelView set(NodePart part){
            trns.set(part.node.worldTrns);
            mesh = part.mesh;
            material = part.material;
            return this;
        }
    }
}
