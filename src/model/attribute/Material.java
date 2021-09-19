package model.attribute;

import arc.func.*;
import arc.struct.*;
import model.*;
import model.attribute.Attribute.*;

/**
 * A material is a combined set of {@link Attribute}s that participate in a rendering of a {@link Model}. Typically,
 * a material is also used as a base definition for shaders, using pre-processors.
 */
@SuppressWarnings("unchecked")
public class Material{
    /** The material ID. */
    public String id;

    private long mask;
    private final LongMap<Attribute<?, ?>> attributes = new LongMap<>();

    public Material(Attribute<?, ?>... attributes){
        set(attributes);
    }

    /** Gets an {@link Attribute} using its attribute alias. */
    public <T extends Attribute<T, V>, V extends AttrAlias<T>> T get(V alias){
        return (T)attributes.get(alias.id());
    }

    /** Sets an {@link Attribute} to the material and returns the previously associated attribute, if any. */
    public <T extends Attribute<T, ?>> T set(T attr){
        long id = attr.alias.id();

        mask |= id;
        return (T)attributes.put(id, attr);
    }

    /** Sets multiple {@link Attribute}s at once. */
    public void set(Attribute<?, ?>... attributes){
        for(var attr : attributes){
            set(attr);
        }
    }

    /** Sets multiple {@link Attribute}s at once. */
    public void set(Iterable<? extends Attribute<?, ?>> attributes){
        for(var attr : attributes){
            set(attr);
        }
    }

    /** Removes an attribute using its alias. */
    public <T extends Attribute<T, V>, V extends AttrAlias<T>> T remove(V alias){
        long id = alias.id();

        mask &= ~id;
        return (T)attributes.remove(id);
    }

    /** Removes multiple {@link Attribute}s using its alias at once. */
    public void remove(AttrAlias<?>... aliases){
        for(var alias : aliases){
            remove(alias);
        }
    }

    /** Removes multiple {@link Attribute}s using its alias at once. */
    public void remove(Iterable<? extends AttrAlias<?>>... aliases){
        for(var alias : aliases){
            remove(alias);
        }
    }

    /** @return Whether this material has an attribute with the specified alias. */
    public boolean has(AttrAlias<?> alias){
        return (mask & alias.id()) == alias.id();
    }

    /** Applies a consumer to all {@link Attribute}s that this material has. */
    public void each(Cons<Attribute<?, ?>> cons){
        for(int i = 0; i < 64; i++){
            var attr = attributes.get(1L << i);
            if(attr != null) cons.get(attr);
        }
    }

    /** @return The combined {@code long} mask of this material. */
    public long mask(){
        return mask;
    }
}
