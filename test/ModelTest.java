import arc.*;
import arc.backend.sdl.*;
import org.junit.jupiter.api.*;

public class ModelTest{
    void app(ApplicationListener listener){
        new SdlApplication(listener, new SdlConfig(){{
            depth = 16;
            decorated = false;
            maximized = true;
            title = "Model Test";
        }});
    }

    @Test
    public void createModel(){
        app(new ApplicationListener(){});
    }
}
