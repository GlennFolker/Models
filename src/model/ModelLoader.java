package model;

import arc.assets.*;
import arc.assets.loaders.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import model.ModelLoader.*;

@SuppressWarnings("rawtypes")
public class ModelLoader extends SynchronousAssetLoader<Model, ModelParameters>{
    public final BaseJsonReader reader;

    public ModelLoader(FileHandleResolver resolver, BaseJsonReader reader){
        super(resolver);
        this.reader = reader;
    }

    @Override
    public Model load(AssetManager assetManager, String fileName, Fi file, ModelParameters parameter){
        var target = parameter != null && parameter.model != null ? parameter.model : new Model();
        target.load(reader.parse(file));

        return target;
    }

    @Override
    public Seq<AssetDescriptor> getDependencies(String fileName, Fi file, ModelParameters parameter){
        return null;
    }

    public static class ModelParameters extends AssetLoaderParameters<Model>{
        public @Nullable Model model;

        public ModelParameters(){}

        public ModelParameters(Model model){
            this.model = model;
        }
    }
}
