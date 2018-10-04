package io.anuke.mindustry.graphics;

import io.anuke.mindustry.game.EventType.WorldLoadGraphicsEvent;
import io.anuke.ucore.core.Events;

public class FloorRenderer{
    private final static int chunksize = 64;

    public FloorRenderer(){
        Events.on(WorldLoadGraphicsEvent.class, event -> clearTiles());
    }

    public void drawFloor(){

    }

    public void beginDraw(){

    }

    public void endDraw(){

    }

    public void drawLayer(CacheLayer layer){

    }

    private void cacheChunk(int cx, int cy){

    }

    public void clearTiles(){

    }

    private class Chunk{

    }
}
