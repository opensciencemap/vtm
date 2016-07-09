package org.oscim.layers.tile.vector.labeling;

import org.oscim.core.MapPosition;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.TextBucket;
import org.oscim.renderer.bucket.TextureBucket;

final class LabelTask {

    final TextureBucket layers;
    final TextBucket textLayer;
    final SymbolBucket symbolLayer;

    final MapPosition pos;

    LabelTask() {
        pos = new MapPosition();

        symbolLayer = new SymbolBucket();
        textLayer = new TextBucket();

        layers = symbolLayer;
        symbolLayer.next = textLayer;
    }

}
