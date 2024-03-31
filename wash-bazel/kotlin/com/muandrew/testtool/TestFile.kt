package com.muandrew.testtool

import com.google.devtools.build.runfiles.Runfiles
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

object TestFiles {

    /**
     * This call can be expensive so get the testdata directory path then build off of that.
     * @param path for example jvm/com/muandrew/stock/testdata/
     */
    fun testDirectoryPath(path: String): String {
        // https://github.com/bazelbuild/bazel/blob/master/tools/java/runfiles/Runfiles.java
        val runfiles = Runfiles.create()
        return Paths.get(runfiles.rlocation("__main__/$path")).toString()
    }

    fun Path.readAsFileToString(): String {
        val br = Files.newBufferedReader(this)
        return br.lines().collect(Collectors.joining())
    }
}
