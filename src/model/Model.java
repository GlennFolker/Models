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
import model.part.MeshPair.*;
import model.part.Node.*;

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
            var pair = new MeshPair();

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

            for(var partData = data.require("parts").child; partData != null; partData = partData.next){
                var part = pair.new MeshPart();
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
                part.count = indices.size;
            }

            var mesh = new Mesh(true, vertices.length / attr.sum(a -> a.components), indices.size, attr.toArray());
            mesh.setVertices(vertices);
            mesh.setIndices(indices.toArray());

            pair.mesh = mesh;
            meshes.add(pair);
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
                if(data.has(name)){
                    mat.set(new ColAttr(alias,
                        data.getFloat(0),
                        data.getFloat(1),
                        data.getFloat(2),
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

            materials.add(mat);
        }
    }

    protected void loadNodes(JsonValue json){
        for(var data = json.child; data != null; data = json.next){
            var node = new Node();
            node.id = data.require("id").asString();

            var trns = data.get("translation");
            if(trns != null && trns.isArray()){
                node.translation.set(
                    trns.getFloat(0),
                    trns.getFloat(1),
                    trns.getFloat(2)
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

            var scl = data.get("translation");
            if(scl != null && scl.isArray()){
                node.scaling.set(
                    scl.getFloat(0),
                    scl.getFloat(1),
                    scl.getFloat(2)
                );
            }

            for(var partData = data.require("parts").child; partData != null; partData = partData.next){
                var part = node.new NodePart();
                part.mesh = meshPart(partData.require("meshpartid").asString());
                part.material = material(partData.require("materialid").asString());

                node.parts.add(part);
            }

            nodes.add(node);
        }
    }

    @Override
    public void dispose(){
        meshes.each(Disposable::dispose);

        meshes.clear();
        nodes.clear();
        materials.clear();
    }

    /** @return The {@link MeshPart} with the specified ID, or null if there are none. */
    public MeshPart meshPart(String id){
        for(var mesh : meshes){
            var part = mesh.parts.find(p -> p.id.equals(id));
            if(part != null) return part;
        }
        return null;
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

    /** A packed renderable view of a {@link Model} used in {@link ModelShader} to specify renderings. */
    public static class ModelView implements Poolable{
        /** The transform matrix of this view. */
        public final Mat3D trns = new Mat3D();
        /** The {@link MeshPart} of this model to be rendered by the shader. */
        public MeshPart mesh;
        /** The {@link Material} of this view. */
        public Material material;

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
