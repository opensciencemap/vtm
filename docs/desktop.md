### Desktop

To run the desktop samples can use the Gradle `run` task.

e.g. for `vtm-playground` can change the [mainClassName](../vtm-playground/build.gradle) to run each sample and pass args:
```groovy
./gradlew :vtm-playground:run -Pargs=/path/to/map
```
