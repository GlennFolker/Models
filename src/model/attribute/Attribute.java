package model.attribute;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import model.attribute.Attribute.*;
import model.attribute.Attribute.BlendAttr.*;
import model.attribute.Attribute.ColAttr.*;
import model.attribute.Attribute.FAttr.*;
import model.attribute.Attribute.TexAttr.*;

/** Defines an attribute type to be used in a {@link Material}. These attributes have a pre-registered aliases. */
public abstract class Attribute<T, A extends AttrAlias<T>>{
    private static final Seq<String> registered = new Seq<>();

    /** The alias of this attribute that contains name and an identifier, used for mapping and masking. */
    public final A alias;

    /** Basic attribute instantiation. The passed ID parameter must responsibly correlate to one of the supported IDs. */
    public Attribute(A alias){
        this.alias = alias;
    }

    /** Registers an attribute by its alias. Returns a new identifier if unknown, and existing identifier otherwise. */
    public static long register(String alias){
        int i = registered.indexOf(alias::equals);
        if(i != -1){
            return 1L << (i - 1);
        }else{
            registered.add(alias);
            return 1L << (registered.size - 1);
        }
    }

    /** Applies necessary uniforms of this attribute to the given shader. */
    public void apply(Shader shader){}

    /** Specifies an alias for materials to be used in shader uniforms and provides a registered ID for the attribute. */
    public interface AttrAlias<V>{
        /** @return The name of this alias, typically overridden by {@link Enum#name()}. */
        String name();

        /** @return The mask of this attribute alias. */
        long id();

        /** @return The uniform name of this attribute alias. */
        default String uniform(){
            return "u_" + name();
        }

        /** @return The pre-processor flag name of this attribute alias. */
        default String flag(){
            return name() + "Flag";
        }
    }

    /** Defines an {@link Attribute} type containing a single {@code float} value. */
    public static class FAttr extends Attribute<FAttr, FAlias>{
        public final float value;

        public FAttr(FAlias alias, float value){
            super(alias);
            this.value = value;
        }

        @Override
        public void apply(Shader shader){
            shader.setUniformf(alias.uniform(), value);
        }

        /** Defines all supported attribute aliases for a {@link FAttr}. */
        public enum FAlias implements AttrAlias<FAttr>{
            shininess,
            alphaTest;

            public static final FAlias[] all = values();

            public final long id = register(name());

            @Override
            public long id(){
                return id;
            }
        }
    }

    /** Defines an {@link Attribute} type containing a single {@link Color} value. */
    public static class ColAttr extends Attribute<ColAttr, ColAlias>{
        public final Color value = Color.white.cpy();

        public ColAttr(ColAlias alias, Color value){
            this(alias, value.r, value.g, value.b, value.a);
        }

        public ColAttr(ColAlias alias, float r, float g, float b, float a){
            super(alias);
            value.set(r, g, b, a);
        }

        @Override
        public void apply(Shader shader){
            shader.setUniformf(alias.uniform(), value);
        }

        /** Defines all supported attribute aliases for a {@link ColAttr}. */
        public enum ColAlias implements AttrAlias<ColAttr>{
            diffuse,
            specular,
            emissive,
            ambientLight;

            public static final ColAlias[] all = values();

            public final long id = register(name());

            @Override
            public long id(){
                return id;
            }

            @Override
            public String uniform(){
                return "u_" + name() + "Color";
            }

            @Override
            public String flag(){
                return name() + "ColorFlag";
            }
        }
    }

    /** Defines an {@link Attribute} type containing a {@link Texture} along with its UV mapping. */
    public static class TexAttr extends Attribute<TexAttr, TexAlias>{
        /** Texture name, used to optionally remap the texture value and UV mappings. */
        public String name;
        /** The bound texture, can be {@link #remap()}ed. */
        public Texture value;
        public float u = 0f;
        public float v = 0f;
        public float u2 = 1f;
        public float v2 = 1f;

        public TexAttr(TexAlias alias, @Nullable String name){
            this(alias, name, 0f, 0f, 1f, 1f);
        }

        public TexAttr(TexAlias alias, @Nullable Texture texture){
            this(alias, texture, null, 0f, 0f, 1f, 1f);
        }

        public TexAttr(TexAlias alias, TextureRegion reg){
            this(alias, reg.texture, reg instanceof AtlasRegion at ? at.name : null, reg.u, reg.v, reg.u2, reg.v2);
        }

        public TexAttr(TexAlias alias, @Nullable String name, float u, float v, float u2, float v2){
            this(alias, null, name, u, v, u2, v2);
        }

        public TexAttr(TexAlias alias, @Nullable Texture value, String name, float u, float v, float u2, float v2){
            super(alias);
            set(value, name, u, v, u2, v2);
        }

        public void set(TextureRegion reg){
            set(reg.texture, reg instanceof AtlasRegion at ? at.name : null, reg.u, reg.v, reg.u2, reg.v2);
        }

        public void set(@Nullable Texture value, @Nullable String name, float u, float v, float u2, float v2){
            this.value = value;
            this.name = name;
            this.u = u;
            this.v = v;
            this.u2 = u2;
            this.v2 = v2;
        }

        /** Remaps this texture to use {@link Core#atlas}' texture presumably after pixmap packing, if applicable. */
        public void remap(){
            if(name == null) return;

            var tex = Core.atlas.find(name);
            if(tex != null && tex.found() && tex.texture != value){
                value = tex.texture;

                u = Mathf.map(u, 0f, 1f, tex.u, tex.u2);
                u2 = Mathf.map(u2, 0f, 1f, tex.u, tex.u2);
                v = Mathf.map(v, 0f, 1f, tex.v, tex.v2);
                v2 = Mathf.map(v2, 0f, 1f, tex.v, tex.v2);
            }
        }

        /**
         * @inheritDocs
         * Calls to this function must be ordered by the ordinal of the {@link TexAlias}.
         */
        @Override
        public void apply(Shader shader){
            int val = TexAlias.all.length - alias.ordinal();

            value.bind(val);
            shader.setUniformi(alias.uniform(), val);
        }

        /** Defines all supported attribute aliases for a {@link TexAttr}. */
        public enum TexAlias implements AttrAlias<TexAttr>{
            diffuse,
            specular,
            emissive;

            public static final TexAlias[] all = values();

            public final long id = register(name());

            @Override
            public long id(){
                return id;
            }

            @Override
            public String uniform(){
                return "u_" + name() + "Texture";
            }

            @Override
            public String flag(){
                return name() + "TextureFlag";
            }
        }
    }

    /** Defines an {@link Attribute} type containing blending source and destination function values. */
    public static class BlendAttr extends Attribute<BlendAttr, BlendAlias>{
        /** Specifies how incoming colors are computed. Defaults to {@link Gl#srcAlpha}. */
        public int src;
        /** Specifies how existing colors are computed. Defaults to {@link Gl#oneMinusSrcAlpha}. */
        public int dst;

        public BlendAttr(){
            this(Blending.normal);
        }

        public BlendAttr(Blending blend){
            this(blend.src, blend.dst);
        }

        public BlendAttr(int src, int dst){
            super(BlendAlias.blended);
            this.src = src;
            this.dst = dst;
        }

        /** Defines all supported attribute aliases for a {@link BlendAttr}. */
        public enum BlendAlias implements AttrAlias<BlendAttr>{
            blended;

            public final long id = register(name());

            @Override
            public long id(){
                return id;
            }
        }
    }
}
