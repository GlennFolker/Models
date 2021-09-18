package model.attribute;

import arc.struct.*;

public class Material{
    private long mask;
    private final LongMap<Attribute> attributes = new LongMap<>();
}
