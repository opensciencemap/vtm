### iOS implementation

RoboVm needs the native libs / frameworks to create a build.
Copy those files from `vtm-ios-[CURRENT-VERSION]-natives.jar` into a temp folder.

Create a copy task into your **build.gradle**.

```groovy
task copyFrameWorks(type: Copy) {
    from(zipTree("./libs/vtm-ios-[CURRENT-VERSION]-natives.jar"))
    into("${buildDir}/native")
}

tasks.withType(org.gradle.api.tasks.compile.JavaCompile) {
    compileTask -> compileTask.dependsOn copyFrameWorks
}
```

Now you can configure your `robovm.xml` to implement the vtm-natives and the SVG-Framework.

```xml
<libs>
    <lib>z</lib>
    <lib>build/native/libvtm-jni.a</lib>  <!--vtm native -->
</libs>
<frameworkPaths>
    <path>build/native</path>  <!--SVGgh framework path -->
</frameworkPaths>
<frameworks>
    <framework>SVGgh</framework> <!--SVGgh framework name -->
    <framework>UIKit</framework>
    <framework>OpenGLES</framework>
    <framework>QuartzCore</framework>
    <framework>CoreGraphics</framework>
    <framework>OpenAL</framework>
    <framework>AudioToolbox</framework>
    <framework>AVFoundation</framework>
</frameworks>
```

Remember the implementation of a iOS framework is possible since iOS 8.
So we must set the min iOS-Version at `Info.plist.xml`.

```xml
<dict>
    <key>MinimumOSVersion</key>
    <string>8.0</string>
    ...
```
