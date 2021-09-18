package model;

import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import model.attribute.*;
import model.part.*;

public class Model implements Disposable{
    /** The model ID. */
    public String id = "";

    /** All the {@link Mesh}es that this model contains. */
    public final Seq<Mesh> meshes = new Seq<>();
    /** All the {@link MeshPart}s that this model contains. */
    public final Seq<MeshPart> meshParts = new Seq<>();
    /** All the {@link Material}s that this model contains. */
    public final Seq<Material> materials = new Seq<>();

    @Override
    public void dispose(){

    }
}
