package org.oscim.jni;


import com.badlogic.gdx.jnigen.AntScriptGenerator;
import com.badlogic.gdx.jnigen.BuildConfig;
import com.badlogic.gdx.jnigen.BuildTarget;
import com.badlogic.gdx.jnigen.BuildTarget.TargetOs;

public class JniBuilder {
	public static void main(String[] args) {
		String[] headers = { "." };
		String[] sources = { "gl/utils.c", "triangle/TriangleJni.c", "triangle/triangle.c", "triangle/triangle_dbg.c"  };
		String cflags = " -Wall -std=c99 -O2 -DTRILIBRARY -DREDUCED -DCDT_ONLY -DNO_TIMER"; // -Werror";

		BuildTarget win32home = BuildTarget.newDefaultTarget(TargetOs.Windows,
				false);
		win32home.compilerPrefix = "";
		win32home.buildFileName = "build-windows32home.xml";
		win32home.excludeFromMasterBuildFile = true;
		win32home.headerDirs = headers;
		win32home.cIncludes = sources;
		win32home.cFlags += cflags;
		win32home.cppFlags += cflags;

		BuildTarget win32 = BuildTarget.newDefaultTarget(TargetOs.Windows,
				false);
		win32.headerDirs = headers;
		win32.cIncludes = sources;
		win32.cFlags += cflags;
		win32.cppFlags += cflags;

		BuildTarget win64 = BuildTarget
				.newDefaultTarget(TargetOs.Windows, true);
		win64.headerDirs = headers;
		win64.cIncludes = sources;
		win64.cFlags += cflags;
		win64.cppFlags += cflags;

		BuildTarget lin32 = BuildTarget.newDefaultTarget(TargetOs.Linux, false);
		lin32.headerDirs = headers;
		lin32.cIncludes = sources;
		lin32.cFlags += cflags;
		lin32.cppFlags += cflags;

		BuildTarget lin64 = BuildTarget.newDefaultTarget(TargetOs.Linux, true);
		lin64.headerDirs = headers;
		lin64.cIncludes = sources;
		lin64.cFlags += cflags;
		lin64.cppFlags += cflags;

		// BuildTarget mac = BuildTarget.newDefaultTarget(TargetOs.MacOsX,
		// false);
		// mac.headerDirs = headers;
		// mac.cIncludes = sources;
		// mac.cFlags += cflags;
		// mac.cppFlags += cflags;
		// mac.linkerFlags += " -framework CoreServices -framework Carbon";

		BuildTarget android = BuildTarget.newDefaultTarget(TargetOs.Android,
				false);
		android.headerDirs = headers;
		android.cIncludes = sources;
		android.cFlags += cflags;
		android.cppFlags += cflags;
		android.linkerFlags += " -llog";
		// BuildTarget ios = BuildTarget.newDefaultTarget(TargetOs.IOS, false);
		// ios.headerDirs = headers;
		// ios.cIncludes = sources;
		// ios.cFlags += cflags;
		// ios.cppFlags += cflags;

		//new NativeCodeGenerator().generate();
		new AntScriptGenerator().generate(new BuildConfig("vtm-jni"),
				//win32home, win32, win64, lin32,
				lin64, android);
//		BuildExecutor.executeAnt("jni/build-windows32home.xml", "-v clean");
//		BuildExecutor.executeAnt("jni/build-windows32home.xml", "-v");
//		BuildExecutor.executeAnt("jni/build.xml", "pack-natives -v");
	}
}
