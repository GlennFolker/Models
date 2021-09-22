package model;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.struct.*;
import model.Model.*;
import model.attribute.*;

/**
 * A shader to dynamically render model node parts. These shaders are constructed with the combined attribute mask
 * typically returned by {@link Material#mask()}.
 */
public class ModelShader extends Shader{
    public static RenderType type = RenderType.def;
    public static Func<String, Fi> provider = Core.files::internal;

    private static String defVert, defFrag;
    private static final LongMap<ModelShader> shaders = new LongMap<>();

    /** The renderable model view that this shader should use in {@link #apply()}. */
    public ModelView model;
    /** The 3D camera that this shader should use in {@link #apply()}. */
    public Camera3D cam;

    /** Call this method to initialize the values of {@link #defVert} and {@link #defFrag}. */
    public static void init(){
        defVert = provider.get("model.vert").readString();
        defFrag = provider.get("model.frag").readString();
    }

    /** Gets or constructs a shader using the specified {@link ModelView}. */
    public static ModelShader get(ModelView view){
        if(defVert == null || defFrag == null) throw new IllegalStateException("Call init() first.");
        long mask = view.material.mask();

        if(!shaders.containsKey(mask)) shaders.put(mask, new ModelShader(prefix(view.material)));
        return shaders.get(mask);
    }

    private static String prefix(Material material){
        var builder = new StringBuilder();
        material.each(attr -> builder.append("#define ").append(attr.alias.flag()).append(";\n"));

        return builder.toString();
    }

    private ModelShader(String prefix){
        super(prefix + defVert, prefix + defFrag);
    }

    @Override
    public void dispose(){
        super.dispose();

        long key = shaders.findKey(this, true, -1);
        if(key != -1) shaders.remove(key);
    }

    @Override
    public void apply(){
        setUniformi("u_renderType", type.ordinal());
        setUniformMatrix4("u_proj", cam.combined.val);
        setUniformMatrix4("u_trans", model.trns.val);
        setUniformf("u_camPos", cam.position);
        setUniformf("u_res", Core.graphics.getWidth(), Core.graphics.getHeight());
        setUniformf("u_scl", cam.width / Core.graphics.getWidth(), cam.height / Core.graphics.getHeight());

        model.material.each(attr -> attr.apply(this));
    }

    public enum RenderType{
        def,
        hybrid2D
    }
}
