#!/bin/bash
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
echo yes | android update sdk --filter platform-tools --no-ui
echo yes | android update sdk --filter android-21 --no-ui
echo yes | android update sdk --filter extra-android-support --no-ui
echo yes | android update sdk --filter extra-android-m2repository --no-ui
