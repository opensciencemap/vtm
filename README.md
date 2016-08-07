# V™
<br/>


**Development continues at https://github.com/mapsforge/vtm**
#<br/>
VTM is developed within the [OpenScienceMap](http://opensciencemap.org) project.

## Features
- Java map library
- OpenGL vector-tile rendering
- Themeable vector layers
- Support for multiple tile sources:
  - primary opensciencemap (.vtm)
  - mapsforge files
  - GeoJSON tiles
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
- **vtm-gdx** common libgdx backend code
- **vtm-android-gdx** Android application
- **vtm-desktop** Desktop application
- **vtm-web** HTML5/GWT backend
- **vtm-web-app** HTML5/GWT application
- **vtm-ios** iOS application

The libgdx backends for GWT and iOS are very experimental. 


## Getting started

`git clone --recursive https://github.com/opensciencemap/vtm`

Install Android SDK and build-tools 20.0. From extras add
'Android Support Library/Repository' and 'Google Repository'.
The commands below should set things up correctly when Android
SDK is already installed.
```
export ANDROID_HOME=/path/to/your/android-sdk
./android-stuff.sh
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
`./gradlew :vtm-desktop:run` <br/>to run the desktop demo (only Linux64 and Win64 native libs are provided atm)<br/>
`./gradlew :vtm-web-app:jettyDraftWar` <br/>to run the webgl demo<br/>


## WebGL Demo
[OpenScienceMap](http://opensciencemap.org/s3db/#scale=17,rot=61,tilt=51,lat=53.075,lon=8.807) view of Bremen.
- hold right mouse button to change view direction


## Credits
This library contains code from several projects:
- **mapsforge**: based on 0.2.4 (http://mapsforge.org)
- **osmdroid**: some overlay classes (http://code.google.com/p/osmdroid/)
- **libgdx**: AsyncTask, MathUtils and Interpolation classes (https://github.com/libgdx)
- **Android**: some Matrix code, TimSort (http://source.android.com)
- **tessellate**: (https://github.com/cscheid/tessellate)

