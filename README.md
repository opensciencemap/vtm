## A Vector Tile Map Library

VectorTileMap is part of the OpenScienceMap project, developed at University of Bremen.
http://www.opensciencemap.org

### Features
- Java map library
- OpenGL vector-tile rendering
- Themeable vector layers
- Support for multiple tile sources: primary opensciencemap (.vtm)
  - vector: partially mapbox vector-tile and mapsforge files
  - bitmap: any quadtree-scheme tiles as texture
- Backends for Android, Desktop and HTML5/WebGL (through libgdx and GWT)

### Getting started
```
git clone https://github.com/opensciencemap/vtm
cd vtm
git submodule init && git submodule update
ln -s ../../vtm/assets/styles vtm-android-app/assets/styles
ln -s ../../vtm/assets/patterns vtm-android-app/assets/patterns
```

Then import projects into eclipse. To set up a tile server for .vtm tiles see https://github.com/opensciencemap/TileStache/tree/master/TileStache/OSciMap4

### Projects
- **vtm** contains the core library
- **vtm-android** Android backend - (no libgdx required)
- **vtm-android-example** provides examples using **vtm-android**
- **vtm-gdx** common libgdx backend code
- **vtm-gdx-desktop** Desktop application 
- **vtm-gdx-html** HTML5/GWT application
- **vtm-gdx-android** Android application using libgdx backend

### WebGL Demo
http://opensciencemap.org/map/#scale=17,rot=61,tilt=51,lat=53.075,lon=8.807
- hold middle mouse button to change view direction
- Keys
  - g - toggle tile-grid layer
  - d - default theme
  - t - tubes theme


### Credits
This library contains code from several projects:
- mapsforge: based on 0.2.4 (http://mapsforge.org)
- osmdroid: some overlay classes (http://code.google.com/p/osmdroid/)
- libgdx: AsyncTask, MathUtils and Interpolation classes (https://github.com/libgdx)
- Android: some Matrix code, TimSort (http://source.android.com)
- tessellate: (https://github.com/cscheid/tessellate)
