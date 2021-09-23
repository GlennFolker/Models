package model.attribute;

import model.attribute.LightsAttr.*;
import model.attribute.LightsAttr.AmbLightsAttr.*;
import model.attribute.LightsAttr.DirLightsAttr.*;

/** A type of {@link Material} that is used extensively for lighting. */
public class Environment extends Material{
    /** Adds a light instance to this environment, automatically adding the necessary attribute if haven't set up. */
    public <T extends Lights<T>> void add(T light){
        if(light instanceof AmbLights l){
            if(!has(AmbLightsAlias.ambLights)) set(new AmbLightsAttr());
            get(AmbLightsAlias.ambLights).values.add(l);
        }else if(light instanceof DirLights l){
            if(!has(DirLightsAlias.dirLights)) set(new DirLightsAttr());
            get(DirLightsAlias.dirLights).values.add(l);
        }
    }

    /** Removes a light instance from this environment. */
    public <T extends Lights<T>> void remove(T light){
        if(light instanceof AmbLights l){
            if(!has(AmbLightsAlias.ambLights)) return;
            get(AmbLightsAlias.ambLights).values.remove(l);
        }else if(light instanceof DirLights l){
            if(!has(DirLightsAlias.dirLights)) return;
            get(DirLightsAlias.dirLights).values.remove(l);
        }
    }

    /** @return The amount of {@link DirLights} this environment has, for convenience. */
    @SuppressWarnings("all")
    public int numDirLights(){
        Object[] items; // Type erasures suck.
        return has(DirLightsAlias.dirLights) ? (items = get(DirLightsAlias.dirLights).values.items).length : 0;
    }
}
