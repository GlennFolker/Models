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
        }
    }

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
