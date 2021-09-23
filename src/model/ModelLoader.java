package model;

import arc.assets.*;
import arc.assets.loaders.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import model.ModelLoader.*;

/** An {@link AssetLoader} for loading {@link Model} instances. Models that are to be loaded can be bound in {@link ModelParameters}. */
@SuppressWarnings("rawtypes")
public class ModelLoader extends SynchronousAssetLoader<Model, ModelParameters>{
    /** The JSON reader of this asset loader, either textual or binary. */
    public final BaseJsonReader reader;

    /** Constructs a model loader using a {@link FileHandleResolver} and a JSON reader. */
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

    /** An {@link AssetLoaderParameters} specifically to bind loaded {@link Model}. */
    public static class ModelParameters extends AssetLoaderParameters<Model>{
        /** The model instance to be loaded to. If null, a new model will be instantiated. */
        public @Nullable Model model;

        /** Constructs a model parameter, given model might be null. */
        public ModelParameters(Model model){
            this.model = model;
        }
    }
}
