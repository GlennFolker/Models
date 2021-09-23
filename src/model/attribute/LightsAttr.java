package model.attribute;

import arc.graphics.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import model.attribute.Attribute.*;
import model.attribute.LightsAttr.*;
import model.attribute.LightsAttr.AmbLightsAttr.*;
import model.attribute.LightsAttr.DirLightsAttr.*;

/** An {@link Attribute} specified for lights. This attribute type defines a {@link Seq} of the generic lights. */
public abstract class LightsAttr<T extends Lights<T>, V extends LightsAttr<T, V, A>, A extends AttrAlias<V>> extends Attribute<V, A>{
    /** All the lights that this attribute has. */
    public final Seq<T> values = new Seq<>();

    @SafeVarargs
    protected LightsAttr(A alias, T... lights){
        super(alias);
        values.addAll(lights);
    }

    protected LightsAttr(A alias, Iterable<T> lights){
        super(alias);
        values.addAll(lights);
    }

    @Override
    public StringBuilder preprocess(StringBuilder builder){
        Object[] items = values.items;
        return builder.append("#define num").append(Strings.capitalize(alias.name())).append(' ').append(items.length).append("\n");
    }

    @Override
    public void apply(Shader shader){
        shader.setUniformi(alias.uniform() + "Size", values.size);
    }

    /** The light type to be used in {@link #values}. Always has color; extend this class to implement other behaviors. */
    public static abstract class Lights<T extends Lights<T>>{
        /** The {@link Color} of this light. */
        public final Color color = new Color();

        /** @return An exact copy of this light. */
        public abstract T copy();
    }

    /** Defines a {@link LightsAttr} type containing {@link AmbLights}. */
    public static class AmbLightsAttr extends LightsAttr<AmbLights, AmbLightsAttr, AmbLightsAlias>{
        public AmbLightsAttr(AmbLights... lights){
            super(AmbLightsAlias.ambLights, lights);
        }

        public AmbLightsAttr(Iterable<AmbLights> lights){
            super(AmbLightsAlias.ambLights, lights);
        }

        @Override
        public StringBuilder preprocess(StringBuilder builder){
            return builder.append("#define ").append(alias.flag()).append('\n');
        }

        @Override
        public void apply(Shader shader){
            if(values.isEmpty()) return;

            Tmp.c1.set(0f, 0f, 0f, 0f);
            for(var l : values){
                Tmp.c1.r += l.color.r;
                Tmp.c1.g += l.color.g;
                Tmp.c1.b += l.color.b;
                Tmp.c1.a += l.color.a;
            }

            Tmp.c1.r /= values.size;
            Tmp.c1.g /= values.size;
            Tmp.c1.b /= values.size;
            Tmp.c1.a /= values.size;

            shader.setUniformf(alias.uniform(), Tmp.c1);
        }

        @Override
        public AmbLightsAttr copy(){
            return new AmbLightsAttr(values);
        }

        /** An ambient light. Affects all models globally, no matter what. */
        public static class AmbLights extends Lights<AmbLights>{
            public AmbLights(){}

            public AmbLights(Color color){
                set(color.r, color.g, color.b, color.a);
            }

            public AmbLights(float r, float g, float b, float a){
                set(r, g, b, a);
            }

            public AmbLights set(Color color){
                return set(color.r, color.g, color.b, color.a);
            }

            public AmbLights set(float r, float g, float b, float a){
                this.color.set(r, g, b, a);
                return this;
            }

            @Override
            public AmbLights copy(){
                return new AmbLights(color);
            }
        }

        /** Defines all supported attribute aliases for a {@link DirLightsAlias}. */
        public enum AmbLightsAlias implements AttrAlias<AmbLightsAttr>{
            ambLights;

            public final long id = register(name());

            @Override
            public long id(){
                return id;
            }
        }
    }

    /** Defines a {@link LightsAttr} type containing {@link DirLights}. */
    public static class DirLightsAttr extends LightsAttr<DirLights, DirLightsAttr, DirLightsAlias>{
        public DirLightsAttr(DirLights... lights){
            super(DirLightsAlias.dirLights, lights);
        }

        public DirLightsAttr(Iterable<DirLights> lights){
            super(DirLightsAlias.dirLights, lights);
        }

        @Override
        public void apply(Shader shader){
            super.apply(shader);

            var u = alias.uniform();
            for(int i = 0; i < values.size; i++){
                var l = values.get(i);
                var index = u + "[" + i + "].";

                shader.setUniformf(index + "color", l.color);
                shader.setUniformf(index + "dir", l.dir);
            }
        }

        @Override
        public DirLightsAttr copy(){
            return new DirLightsAttr(values.map(DirLights::copy));
        }

        /** A directional light; has no position, only rotation. Affects all models globally based on the faces' normals. */
        public static class DirLights extends Lights<DirLights>{
            /** The direction of this light. */
            public final Vec3 dir = new Vec3();

            public DirLights(){}

            public DirLights(Vec3 dir){
                this.dir.set(dir);
            }

            public DirLights(Color color){
                this.color.set(color);
            }

            public DirLights(Color color, Vec3 dir){
                set(color, dir);
            }

            public DirLights set(Color color, Vec3 dir){
                this.color.set(color);
                this.dir.set(dir);
                return this;
            }

            @Override
            public DirLights copy(){
                return new DirLights(color, dir);
            }
        }

        /** Defines all supported attribute aliases for a {@link DirLightsAlias}. */
        public enum DirLightsAlias implements AttrAlias<DirLightsAttr>{
            dirLights;

            public final long id = register(name());

            @Override
            public long id(){
                return id;
            }
        }
    }
}
