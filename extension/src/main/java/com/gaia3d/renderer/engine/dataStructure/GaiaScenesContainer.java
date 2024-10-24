package com.gaia3d.renderer.engine.dataStructure;

import com.gaia3d.renderer.engine.scene.Camera;
import com.gaia3d.renderer.engine.scene.Projection;
import com.gaia3d.renderer.renderable.RenderableGaiaScene;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class GaiaScenesContainer {
    private Projection projection;
    //private TextureCache textureCache;
    private Camera camera;
    private List<RenderableGaiaScene> renderableGaiaScenes;

    public GaiaScenesContainer(int width, int height) {
        renderableGaiaScenes = new ArrayList<>();
        projection = new Projection(width, height);
        //textureCache = new TextureCache();
        //camera = new Camera();
    }

    public void addRenderableGaiaScene(RenderableGaiaScene renderableGaiaScene) {
        renderableGaiaScenes.add(renderableGaiaScene);
    }
}