package model.attribute;

import model.attribute.Attribute.ColAttr.*;
import model.attribute.LightsAttr.*;
import model.attribute.LightsAttr.DirLightsAttr.*;

/** A type of {@link Material} that is used extensively for lighting. */
public class Environment extends Material{
    @Override
    public <T extends Attribute<T, ?>> T set(T attr){
        var a = attr.alias;
        if(
            a == ColAlias.ambientLight ||
            a == DirLightsAlias.dirLights
        ){
            return super.set(attr);
        }else{
            throw new IllegalArgumentException("Unsupported environment attribute: '" + a + "'");
        }
    }

    /** Adds a light instance to this environment, automatically adding the necessary attribute if haven't set up. */
    public <T extends Lights<T>> void add(T light){
        if(light instanceof DirLights l){
            if(!has(DirLightsAlias.dirLights)) set(new DirLightsAttr());
            get(DirLightsAlias.dirLights).values.add(l);
        }
    }

    /** Removes a light instance from this environment. */
    public <T extends Lights<T>> void remove(T light){
        if(light instanceof DirLights l){
            if(!has(DirLightsAlias.dirLights)) return;
            get(DirLightsAlias.dirLights).values.remove(l);
        }
    }

    /** @return The amount of {@link DirLights} this environment has, for convenience. */
    @SuppressWarnings("all")
    public int numDirLights(){
        Object[] items;
        return has(DirLightsAlias.dirLights) ? (items = get(DirLightsAlias.dirLights).values.items).length : 0;
    }
}
