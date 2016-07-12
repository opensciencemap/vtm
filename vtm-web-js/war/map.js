var mapconfig = {
    zoom : 12,
    latitude : 53.075,
    longitude : 8.8080,
    tileSize : 400
}

function createLayers() {
    var m = map.map();

    // var t = map.loadTheme("DEFAULT");
    // var ts = new vtm.OSciMap4TileSource();
    // var l = new vtm.OsmTileLayer(m);
    // l.setTileSource(ts)
    // l.setRenderTheme(t)

    var s = new vtm.BitmapTileSource("http://a.tile.stamen.com/toner", 0, 18);
    var l = new vtm.BitmapTileLayer(m, s);
    map.addLayer(l)
    // map.addLayer(new vtm.BuildingLayer(m, l))
    // map.addLayer(new vtm.LabelLayer(m, l))

     t = map.loadTheme("TRONRENDER")
     ts = new vtm.OsmLanduseJsonTileSource()
     l = new vtm.VectorTileLayer(m, ts)
     l.setRenderTheme(t)
     map.addLayer(l)
}

function canvasResize() {
    div = document.getElementById('canvas-area')
    canvas = document.getElementById('map-canvas')
    var w = div.scrollWidth;
    var h = div.scrollHeight;
    // console.log(div.clientHeight + " " + div.clientWidth)
    canvas.width = w;
    canvas.height = h;
    canvas.style.width = w + 'px';
    canvas.style.height = h + 'px';
}
canvasResize();
window.addEventListener('resize', canvasResize, false);
