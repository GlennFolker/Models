package model.part;

import arc.graphics.*;
import arc.graphics.gl.*;

/**
 * A {@link Mesh} that stores an ID, rendering primitive type, and indices offset along with length. This part can
 * render itself using a given {@link Shader} and its own indices {@link #offset} and {@link #count}.
 */
public class MeshPart{
    /** The mesh part ID. */
    public String id = "";
    /** The mesh that this part is bound to. Must not be null. */
    public Mesh mesh;

    /** The primitive type of this mesh part, such as {@link Gl#triangles} or {@link Gl#lines}. */
    public int type = Gl.triangles;
    /** The indices array offset of this part. */
    public int offset;
    /** The indices array length of this part. */
    public int count;

    /** Constructs an empty unusable mesh part. Properties must be set before this part can be used. */
    public MeshPart(){}

    /** Constructs a mesh part with the specified {@link Mesh}. {@link #offset} and {@link #count} must be set first. */
    public MeshPart(Mesh mesh){
        this.mesh = mesh;
    }

    /** Renders the {@link #mesh} using the given {@link Shader}, automatically binding it. */
    public void render(Shader shader){
        render(shader, true);
    }

    /** Renders the {@link #mesh} using the given {@link Shader}. */
    public void render(Shader shader, boolean autoBind){
        mesh.render(shader, type, offset, count, autoBind);
    }
}
