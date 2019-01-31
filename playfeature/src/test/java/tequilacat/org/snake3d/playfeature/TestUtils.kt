package tequilacat.org.snake3d.playfeature

import org.junit.Assert

fun assertArraysEqual(v1: FloatArray, v2: FloatArray) {
    Assert.assertEquals(v1.size, v2.size)
    if (! (v1 contentEquals v2)) {
        val s1 = v1.contentToString()
        val s2 = v2.contentToString()

        for(i in v1.indices) {
            Assert.assertEquals(
                "Elem $i: expected = ${v1[i]} real = ${v2[i]} \n#1: $s1 \n#2: $s2",
                v1[i], v2[i]
            )
        }
    }
}
