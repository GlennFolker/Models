import arc.*;
import arc.assets.*;
import arc.backend.sdl.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.serialization.*;
import model.*;
import model.Model.*;
import model.attribute.*;
import model.attribute.LightsAttr.AmbLightsAttr.*;
import model.attribute.LightsAttr.DirLightsAttr.*;
import org.junit.jupiter.api.*;

public class ModelTest{
    void app(ApplicationListener listener){
        new SdlApplication(listener, new SdlConfig(){{
            depth = 16;
            maximized = true;
            title = "Model Test";
        }});
    }

    @Test
    public void generalTest(){
        app(new ApplicationListener(){
            ModelInstance model;
            Anims control;

            Environment env;
            Camera3D cam;

            final Pool<ModelView> pool = Pools.get(ModelView.class, ModelView::new);
            final Seq<ModelView> views = Seq.of(false, 100, ModelView.class);

            @Override
            public void init(){
                Core.assets = new AssetManager();

                Core.assets.setLoader(Model.class, ".g3dj", new ModelLoader(Core.files::internal, new JsonReader()));
                Core.assets.setLoader(Model.class, ".g3db", new ModelLoader(Core.files::internal, new UBJsonReader()));

                cam = new Camera3D();
                Core.assets.load("model.g3dj", Model.class).loaded = e -> {
                    e.init();

                    model = new ModelInstance(e);
                    control = new Anims(model);
                };

                var packer = new PixmapPacker(4096, 4096, 4, true);
                packer.pack("model-tex", new Pixmap(Core.files.internal("model-tex.png")));
                packer.pack("model-emit-tex", new Pixmap(Core.files.internal("model-emit-tex.png")));
                Core.atlas = packer.generateTextureAtlas(TextureFilter.linear, TextureFilter.linear, false);

                env = new Environment();
                env.add(new DirLights().set(Color.white, new Vec3(-1f, -1f, -0.2f)));
                env.add(new AmbLights().set(1f, 1f, 1f, 0.3f));

                ModelShader.init();
                Core.assets.finishLoading();

                Log.infoTag("App", "Initialized.");
            }

            @Override
            public void update(){
                Time.update();

                cam.position.set(0f, 0f, 10f);
                cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
                cam.update();

                control.begin();
                float time = Mathf.slope((Time.time / 600f) % 1f) * 3f - 1f;
                control.animateFrac("node-outer|outer-fold", time);
                control.animateFrac("node-inner|inner-fold", time);
                control.end();

                Core.graphics.clear(0f, 0f, 0f, 0f);
                Gl.depthMask(true);
                Gl.clear(Gl.depthBufferBit);
                Gl.enable(Gl.depthTest);
                Gl.depthFunc(Gl.lequal);

                model.views(pool, views);

                ModelShader prev = null;
                var items = views.items;
                int size = views.size;

                for(int i = 0; i < size; i++){
                    var view = items[i];
                    view.env = env;

                    var shader = ModelShader.get(view);
                    if(shader != prev){
                        shader.bind();
                        prev = shader;
                    }

                    shader.model = view;
                    shader.cam = cam;
                    shader.apply();

                    view.mesh.render(shader, true);
                }

                pool.freeAll(views);
                views.size = 0;

                Gl.disable(Gl.depthTest);
            }
        });
    }
}
