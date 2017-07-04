package org.botellier.log

import java.io.File
import java.util.*

/**
 * Calls the callback function with the folder where
 * the dummy files are contained.
 * @param n the number of dummy segment files to include in the directory.
 */
fun withDummy(n: Int = 0, cb: (File) -> Unit) {
    var folder = getFolderName()
    folder.mkdir()

    for (i in 0..n-1) {
        File(folder, "test-segment-$i").createNewFile()
    }

    cb(folder)
    folder.delete()
}

/**
 * Gets a folder with the name segments-SUFFIX where suffix
 * is a random number.
 */
private fun getFolderName(): File {
    val folder = File("./segments-${Math.abs(Random().nextInt())}")
    if (folder.exists()) {
        return getFolderName()
    } else {
        return folder
    }
}

