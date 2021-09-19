import arc.*;
import arc.backend.sdl.*;
import arc.graphics.*;
import arc.graphics.g3d.*;
import arc.math.geom.*;
import arc.util.*;
import model.*;
import model.Model.*;
import model.attribute.*;
import model.attribute.Attribute.*;
import model.attribute.Attribute.BlendAttr.*;
import model.attribute.Attribute.ColAttr.*;
import model.part.*;
import org.junit.jupiter.api.*;

public class ModelTest{
    Quat q1 = new Quat();

    void app(ApplicationListener listener){
        new SdlApplication(listener, new SdlConfig(){{
            depth = 16;
            maximized = true;
            title = "Model Test";
        }});
    }

    @Test
    public void renderTest(){
        app(new ApplicationListener(){
            ModelView model;
            Camera3D cam;

            @Override
            public void init(){
                model = new ModelView(){{
                    part = new MeshPair(new Mesh(true, 4, 12, VertexAttribute.position3){{
                        setVertices(new float[]{
                            -1f, -1f, 0f,
                            1f, -1f, 0f,
                            1f, 1f, 0f,
                            -1f, 1f, 0f
                        });

                        setIndices(new short[]{0, 1, 2, 2, 3, 0});
                    }}).new MeshPart(){{
                        type = Gl.triangles;
                        offset = 0;
                        count = 6;
                    }};

                    material = new Material(){{
                        set(new ColAttr(ColAlias.diffuse, 1f, 0f, 0f, 1f));
                        set(new BlendAttr(Gl.srcAlpha, Gl.oneMinusSrcAlpha));
                    }};
                }};

                cam = new Camera3D();
                ModelShader.init();
            }

            @Override
            public void update(){
                Time.update();

                cam.position.set(0f, 0f, 10f);
                cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
                cam.update();

                Gl.enable(Gl.depthTest);
                Gl.depthFunc(Gl.lequal);

                Gl.depthMask(true);
                Core.graphics.clear(0f, 0f, 0f, 1f);
                Gl.clear(Gl.depthBufferBit);

                var trns = model.trns;
                trns.set(Tmp.v31.setZero(), q1.set(Vec3.X, Time.time), Tmp.v32.set(1f, 1f, 1f));

                var material = model.material;
                var diff = material.get(ColAlias.diffuse);
                diff.value.shiftHue(Time.time % 1f);

                var blend = material.get(BlendAlias.blended);
                Gl.enable(Gl.blend);
                Gl.blendFunc(blend.src, blend.dst);

                var shader = ModelShader.get(material);
                shader.model = model;
                shader.cam = cam;
                shader.bind();
                shader.apply();

                model.part.render(shader);

                Gl.disable(Gl.depthTest);
                Gl.disable(Gl.blend);
            }
        });
    }
}
