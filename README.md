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
  - Desktop (using libgdx/LwjGL)
  - HTML5/WebGL (using libgdx/GWT)
  - iOS (using libgdx/robovm)

### Projects
- **vtm** contains the core library
- **vtm-android** Android backend  (no libgdx required)
- **vtm-android-example** provides examples using **vtm-android**
- **vtm-android-app** opensciencemap app using **vtm-android**
- **vtm-gdx** common libgdx backend code
- **vtm-android-gdx** Android application
- **vtm-desktop** Desktop application
- **vtm-web** HTML5/GWT backend
- **vtm-web-app** HTML5/GWT application
- **vtm-ios** iOS application

## Getting started

`git clone --recursive https://github.com/opensciencemap/vtm`

Install Android SDK and build-tools 19.1. From extras add
'Android Support Library/Repository' and 'Google Repository'.
The commands below should set things up correctly when Android
SDK is already installed.
```
export ANDROID_HOME=/path/to/your/android-sdk
./android-stuff.sh
```
To use a different  build-tools version X run:
```
find -name build.gradle | xargs sed -i 's/19\.1/19.X/g'
```

### Eclipse
`./gradlew eclipse`<br/>
Import all 'vtm' sub-projects into Eclipse.


### Android-Studio
Just import build.gradle

### Gradle
You can run gradle tasks directly (see also `./gradlew -q tasks`):<br/>
`./gradlew clean install` <br/> to build the libraries and add them to the local maven repository.<br/>
`./gradlew :vtm-android-example:run` <br/> to run the android example<br/>
`./gradlew :vtm-gdx-desktop:run` <br/>to run the desktop demo (only Linux64 and Win64 native libs are provided atm)<br/>
`./gradlew :vtm-gdx-html:jettyDraftWar` <br/>to run the webgl demo<br/>


## WebGL Demo
[OpenScienceMap](http://opensciencemap.org/map/#scale=17,rot=61,tilt=51,lat=53.075,lon=8.807)
- hold right mouse button to change view direction
- Keys: `g` toggle tile-grid layer `d` default- `t` tubes- `r` osmarender-theme



## Credits
This library contains code from several projects:
- **mapsforge**: based on 0.2.4 (http://mapsforge.org)
- **osmdroid**: some overlay classes (http://code.google.com/p/osmdroid/)
- **libgdx**: AsyncTask, MathUtils and Interpolation classes (https://github.com/libgdx)
- **Android**: some Matrix code, TimSort (http://source.android.com)
- **tessellate**: (https://github.com/cscheid/tessellate)
