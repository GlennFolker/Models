package model;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
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
    private static final Mat3D tmp = new Mat3D();

    private final long mask;
    private final int numDirLights;

    /** The renderable model view that this shader should use in {@link #apply()}. */
    public ModelView model;
    /** The 3D camera that this shader should use in {@link #apply()}. */
    public Camera3D cam;

    /** Call this method to initialize the values of {@link #defVert} and {@link #defFrag}. */
    public static void init(){
        defVert = provider.get("shaders/model.vert").readString();
        defFrag = provider.get("shaders/model.frag").readString();
    }

    /** Gets or constructs a shader using the specified {@link ModelView}. */
    public static ModelShader get(ModelView view){
        if(defVert == null || defFrag == null) throw new IllegalStateException("Call init() first.");

        var env = view.env;
        long mask = view.material.mask();

        var shader = shaders.get(mask);
        if(shader == null || !shader.valid(view, env)){
            if(shader != null) shader.dispose();
            shaders.put(mask, shader = new ModelShader(
                prefix(view), mask,
                env == null ? 0 : env.numDirLights()
            ));
        }

        return shader;
    }

    private static String prefix(ModelView view){
        var builder = new StringBuilder();
        view.material.each(attr -> attr.preprocess(builder));
        view.env.each(attr -> attr.preprocess(builder));

        return builder.append('\n').toString();
    }

    private ModelShader(String prefix, long mask, int numDirLights){
        super(prefix + defVert, prefix + defFrag);
        this.mask = mask;
        this.numDirLights = numDirLights;
    }

    public boolean valid(ModelView view, @Nullable Environment env){
        return
            mask == view.material.mask() &&
            (env == null || numDirLights == env.numDirLights());
    }

    @Override
    public void dispose(){
        super.dispose();
        if(shaders.get(mask) == this) shaders.remove(mask);
    }

    @Override
    public void apply(){
        setUniformi("u_renderType", type.ordinal());
        setUniformMatrix4("u_proj", cam.combined.val);
        setUniformMatrix4("u_trans", model.trns.val);
        setUniformf("u_camPos", cam.position);
        setUniformf("u_res", Core.graphics.getWidth(), Core.graphics.getHeight());
        setUniformf("u_scl", cam.width / Core.graphics.getWidth(), cam.height / Core.graphics.getHeight());
        setUniformMatrix4("u_normalMatrix", tmp.set(model.trns).toNormalMatrix().val);

        model.material.each(attr -> attr.apply(this));
        model.env.each(attr -> attr.apply(this));
    }

    public enum RenderType{
        def,
        hybrid2D
    }
}
