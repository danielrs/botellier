package org.botellier.log

import org.junit.Test
import org.junit.Assert
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

class SegmentHeaderTest {
    @Test
    fun writesFixedSizeHeader() {
        val md = MessageDigest.getInstance("MD5")
        val header = SegmentHeader(md)
        header.update(md)

        val output = ByteArrayOutputStream()
        header.writeTo(output)

        val recovered = SegmentHeader.parseFrom(output.toByteArray())
        Assert.assertEquals(0, recovered.id)
        Assert.assertEquals(md.digest().toHexString(), recovered.checksum)
        Assert.assertEquals(1, recovered.totalEntries)
    }

    @Test
    fun messageDigestToHexString() {
        val md = MessageDigest.getInstance("MD5")
        Assert.assertEquals("d41d8cd98f00b204e9800998ecf8427e", md.digest().toHexString())
    }
}
