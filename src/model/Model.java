package model;

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
import model.part.Node.*;

/** A model is a complete set of meshes with specified indices offset and length, model nodes tree, and materials. */
public class Model implements Disposable{
    /** The model ID. */
    public String id = "";

    /** All the {@link Mesh}s that this model contains. */
    protected final Seq<Mesh> meshes = new Seq<>(2);
    /** All the {@link MeshPart}s that this model contains, mapped with their IDs. */
    protected final ObjectMap<String, MeshPart> meshParts = new ObjectMap<>(6);
    /** All the {@link Node}s that this model contains, mapped with their IDs. */
    protected final ObjectMap<String, Node> nodes = new ObjectMap<>(6);
    /** All the {@link Material}s that this model contains, mapped with their IDs. */
    protected final ObjectMap<String, Material> materials = new ObjectMap<>(6);

    /** Loads a model data with the given JSON properties. */
    public void load(JsonValue json){
        dispose();

        id = json.require("id").asString();
        loadMeshes(json.require("meshes"));
        loadMaterials(json.require("materials"));
        loadNodes(json.require("nodes"));
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
                    case "lineLoop" -> Gl.lineLoop;
                    case "lineStrip" -> Gl.lineStrip;
                    case "triangles" -> Gl.triangles;
                    case "triangleStrip" -> Gl.triangleStrip;
                    case "triangleFan" -> Gl.triangleFan;
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
                var name = alias.name();
                if(data.has(name)){
                    mat.set(new FAttr(alias, data.getFloat(name)));
                }
            }

            for(var alias : ColAlias.all){
                var name = alias.name();
                var colData = data.get(name);
                if(colData != null && colData.isArray()){
                    mat.set(new ColAttr(alias,
                        colData.getFloat(0),
                        colData.getFloat(1),
                        colData.getFloat(2),
                        1f
                    ));
                }
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
            if(trns != null && trns.isArray()){
                node.translation.set(
                    trns.getFloat(0) / 100f,
                    trns.getFloat(1) / 100f,
                    trns.getFloat(2) / 100f
                );
            }

            var rot = data.get("rotation");
            if(rot != null && rot.isArray()){
                node.rotation.set(
                    rot.getFloat(0),
                    rot.getFloat(1),
                    rot.getFloat(2),
                    rot.getFloat(3)
                );
            }

            var scl = data.get("scale");
            if(scl != null && scl.isArray()){
                node.scaling.set(
                    scl.getFloat(0) / 100f,
                    scl.getFloat(1) / 100f,
                    scl.getFloat(2) / 100f
                );
            }

            for(var partData = data.require("parts").child; partData != null; partData = partData.next){
                var part = node.new NodePart();
                part.mesh = meshPart(partData.require("meshpartid").asString());
                part.material = material(partData.require("materialid").asString());

                node.parts.add(part);
            }

            node(node);
        }
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
        for(var mat : materials.values()) mat.each(a -> {
            if(a instanceof TexAttr t) t.remap();
        });
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

    /** @return The {@link MeshPart} with the specified ID, or null if there are none. */
    public MeshPart meshPart(String id){
        return meshParts.get(id);
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
            trns.set(part.node().worldTrns);
            mesh = part.mesh;
            material = part.material;
            return this;
        }
    }
}
