## VTM - Very experimental Tile Map library

Future development branch based on https://github.com/opensciencemap/vtm-android

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
- To build the android eclipse project the android ndk plugin is required.
- To set up your own tile server for .vtm tiles see https://github.com/opensciencemap/TileStache/tree/master/TileStache/OSciMap4
- Almost everything is still in early/experimental stage and subject for being rewritten
- pull requests welcome :)


### Credits
This library contains code from several projects:
- mapsforge: based on 0.2.4
- osmdroid: some overlay code
- libgdx: AsyncTask, some modfied classes
- android: some Matrix code
- Triangle: stripped down for simple triangulation (through jni)
