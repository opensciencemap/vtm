#  *\<vector\<tile\>\>map*

<br/>
http://www.opensciencemap.org<br/>
VTM is part of the OpenScienceMap project and developed at University of Bremen.

## Features
- Java map library
- OpenGL vector-tile rendering
- Themeable vector layers
- Support for multiple tile sources: 
  - primary opensciencemap (.vtm)
  - mapsforge files
  - experimental mapnik-vector-tile source
  - bitmap: any quadtree-scheme tiles as texture
- Backends:
  - Android
  - Desktop 
  - HTML5/WebGL (through libgdx and GWT)
  - iOS sooner or later

## Getting started

`git clone --recursive https://github.com/opensciencemap/vtm`

`cd vtm/vtm-android-app/assets && ln -s ../../vtm/assets/* .`


### Projects
- **vtm** contains the core library
- **vtm-android** Android backend  (no libgdx required)
- **vtm-android-example** provides examples using **vtm-android**
- **vtm-android-app** opensciencemap app using **vtm-android**
- **vtm-gdx** common libgdx backend code
- **vtm-gdx-desktop** Desktop application 
- **vtm-gdx-html** HTML5/GWT application
- **vtm-gdx-android** Android application using libgdx backend

### Eclipse
Import all 'vtm' projects that you need into Eclipse.

### Gradle / Android-Studio
Just import build.gradle - should work, not much tested though. <br/>
Or run gradle tasks from the commandline:<br/>
`./gradlew clean install` <br/> to build the libraries and add them to the local maven repository.<br/>
`./gradlew :vtm-android-example:installDebug` <br/> to run the android example<br/>
`./gradlew :vtm-gdx-desktop:run` <br/>to run the desktop demo (only Linux and Win64 or compile the native libs)


## WebGL Demo
http://opensciencemap.org/map/#scale=17,rot=61,tilt=51,lat=53.075,lon=8.807
- hold right mouse button to change view direction
- Keys: `g` toggle tile-grid layer `d` default- `t` tubes- `r` osmarender-theme



## Credits
This library contains code from several projects:
- **mapsforge**: based on 0.2.4 (http://mapsforge.org)
- **osmdroid**: some overlay classes (http://code.google.com/p/osmdroid/)
- **libgdx**: AsyncTask, MathUtils and Interpolation classes (https://github.com/libgdx)
- **Android**: some Matrix code, TimSort (http://source.android.com)
- **tessellate**: (https://github.com/cscheid/tessellate)
