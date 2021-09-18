package model.attribute;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;

/**
 * Defines an attribute type to be used in a {@link Material}. These attributes have a pre-registered identifiers for
 * mapping and masking purposes.
 */
public abstract class Attribute{
    private static final Seq<String> registered = new Seq<>();

    /** The identifier of this attribute, used for mapping and masking. */
    public final long id;

    /** Basic attribute instantiation. The passed ID parameter must responsibly correlate to one of the supported IDs. */
    public Attribute(long id){
        this.id = id;
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

    /** Defines an {@link Attribute} type containing a single {@code float} value. */
    public static class FAttr extends Attribute{
        public static final String shininessAlias = "shininess";
        public static final long shininess = register(shininessAlias);

        public static final String alphaTestAlias = "alphaTest";
        public static final long alphaTest = register(alphaTestAlias);

        public static final long all = shininess | alphaTest;

        public final float value;

        public FAttr(long id, float value){
            super(id);
            if((id & all) == 0L) throw new IllegalArgumentException("Invalid type specified!");

            this.value = value;
        }

        public static FAttr shininess(float value){
            return new FAttr(shininess, value);
        }

        public static FAttr alphaTest(float value){
            return new FAttr(alphaTest, value);
        }
    }

    /** Defines an {@link Attribute} type containing a single {@link Color} value. */
    public static class ColAttr extends Attribute{
        public static final String diffuseAlias = "diffuseColor";
        public static final long diffuse = register(diffuseAlias);

        public static final String specularAlias = "specularColor";
        public static final long specular = register(specularAlias);

        public static final String emissiveAlias = "emissiveColor";
        public static final long emissive = register(emissiveAlias);

        public static final String ambientLightAlias = "ambientLightColor";
        public static final long ambientLight = register(ambientLightAlias);

        public static final long all = diffuse | specular | emissive | ambientLight;

        public final Color value = Color.white.cpy();

        public ColAttr(long id, Color value){
            this(id, value.r, value.g, value.b, value.a);
        }

        public ColAttr(long id, float r, float g, float b, float a){
            super(id);
            if((id & all) == 0L) throw new IllegalArgumentException("Invalid type specified!");

            value.set(r, g, b, a);
        }

        public static ColAttr diffuse(Color color){
            return new ColAttr(diffuse, color);
        }

        public static ColAttr diffuse(float r, float g, float b, float a){
            return new ColAttr(diffuse, r, g, b, a);
        }

        public static ColAttr specular(Color color){
            return new ColAttr(specular, color);
        }

        public static ColAttr specular(float r, float g, float b, float a){
            return new ColAttr(specular, r, g, b, a);
        }

        public static ColAttr emissive(Color color){
            return new ColAttr(emissive, color);
        }

        public static ColAttr emissive(float r, float g, float b, float a){
            return new ColAttr(emissive, r, g, b, a);
        }

        public static ColAttr ambientLight(Color color){
            return new ColAttr(ambientLight, color);
        }

        public static ColAttr ambientLight(float r, float g, float b, float a){
            return new ColAttr(ambientLight, r, g, b, a);
        }
    }

    /** Defines an {@link Attribute} type containing a {@link Texture} along with its UV mapping. */
    public static class TexAttr extends Attribute{
        public static final String diffuseAlias = "diffuseTexture";
        public static final long diffuse = register(diffuseAlias);

        public static final String specularAlias = "specularTexture";
        public static final long specular = register(specularAlias);

        public static final String emissiveAlias = "emissiveTexture";
        public static final long emissive = register(emissiveAlias);

        protected static long all = diffuse | specular | emissive;

        /** Texture name, used to optionally remap the texture value and UV mappings. */
        public String name;
        /** The bound texture, can be {@link #remap()}ed. */
        public Texture value;
        public float u = 0f;
        public float v = 0f;
        public float u2 = 1f;
        public float v2 = 1f;

        public TexAttr(long id, @Nullable String name){
            this(id, name, 0f, 0f, 1f, 1f);
        }

        public TexAttr(long id, @Nullable Texture texture){
            this(id, texture, null, 0f, 0f, 1f, 1f);
        }

        public TexAttr(long id, TextureRegion reg){
            this(id, reg.texture, reg instanceof AtlasRegion at ? at.name : null, reg.u, reg.v, reg.u2, reg.v2);
        }

        public TexAttr(long id, @Nullable String name, float u, float v, float u2, float v2){
            this(id, null, name, u, v, u2, v2);
        }

        public TexAttr(long id, @Nullable Texture value, String name, float u, float v, float u2, float v2){
            super(id);
            if((id & all) == 0L) throw new IllegalArgumentException("Invalid type specified!");

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

        public static TexAttr diffuse(Texture texture){
            return new TexAttr(diffuse, texture);
        }

        public static TexAttr diffuse(TextureRegion region){
            return new TexAttr(diffuse, region);
        }

        public static TexAttr specular(Texture texture){
            return new TexAttr(specular, texture);
        }

        public static TexAttr specular(TextureRegion region){
            return new TexAttr(specular, region);
        }

        public static TexAttr emissive(Texture texture){
            return new TexAttr(emissive, texture);
        }

        public static TexAttr emissive(TextureRegion region){
            return new TexAttr(emissive, region);
        }
    }

    /** Defines an {@link Attribute} type containing blending source and destination function values. */
    public static class BlendAttr extends Attribute{
        public static final String blendAlias = "blended";
        public static final long blend = register(blendAlias);

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
            super(blend);
            this.src = src;
            this.dst = dst;
        }
    }
}
