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

### Notes
- checkout required libraries with 'git submodule init && git submodule update'
- Projects are expected to be build within eclipse (maybe gradle in future)
- To set up your own tile server for .vtm tiles see https://github.com/opensciencemap/TileStache/tree/master/TileStache/OSciMap4

### Projects
- **vtm** contains the core library
- **vtm-android** Android backend - (no libgdx required)
- **vtm-android-example** provides examples using **vtm-android**
- **vtm-gdx** Common libgdx backend code
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
