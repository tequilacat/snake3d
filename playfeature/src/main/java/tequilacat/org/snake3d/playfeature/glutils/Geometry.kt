package tequilacat.org.snake3d.playfeature.glutils

import kotlin.IllegalArgumentException

/**
 * Geometry stored as float array, possibly with indexes.
 * Does not reference OpenGL structures
 */
class Geometry(val vertexes: FloatArray, val vertexCount: Int,
               val indexes: ShortArray, val indexCount: Int,
               val hasNormals: Boolean, val hasTexture: Boolean) {

    fun facetize(): Geometry {
        if (!hasIndexes) throw IllegalArgumentException()

        // triangle count is same but all have new indexes
        val newIndexes = ShortArray(indexCount) { i -> i.toShort() }
        val dstStride = computeVertexStrideInFloats(true, hasTexture)
        val newVertexes = FloatArray(newIndexes.size * dstStride)
        var dstVPos = 0

        for (nv in 0 until indexCount step 3) {
            val dstNormalIndex = dstVPos + dstStride - 3

            for (nvv in 0 until 3) {
                val srcIndex = indexes[nv + nvv]
                val srcVPos = srcIndex * vertexFloatStride

                for (dst in 0 until if (hasTexture) 5 else 3) {
                    newVertexes[dstVPos + dst] = vertexes[srcVPos + dst]
                }

                dstVPos += dstStride
            }

            CoordUtils.crossProduct(
                newVertexes, dstNormalIndex, // end of stride (to account for possible UV in between
                newVertexes, nv, nv+1, nv+2, dstStride
            )
            CoordUtils.normalize(newVertexes, dstNormalIndex, newVertexes, dstNormalIndex)

            newVertexes[dstNormalIndex + dstStride] = newVertexes[dstNormalIndex]
            newVertexes[dstNormalIndex + dstStride + 1] = newVertexes[dstNormalIndex + 1]
            newVertexes[dstNormalIndex + dstStride + 2] = newVertexes[dstNormalIndex + 2]

            newVertexes[dstNormalIndex + dstStride * 2] = newVertexes[dstNormalIndex]
            newVertexes[dstNormalIndex + dstStride * 2 + 1] = newVertexes[dstNormalIndex + 1]
            newVertexes[dstNormalIndex + dstStride * 2 + 2] = newVertexes[dstNormalIndex + 2]
        }

        return Geometry(newVertexes, true, hasTexture, newIndexes)
    }

    constructor(vertexes: FloatArray, hasNormals: Boolean, hasTexture: Boolean,
                indexes: ShortArray = Empty.ShortArray) :
            this(vertexes, vertexes.size / computeVertexStrideInFloats(hasNormals, hasTexture),
                indexes, indexes.size,
                hasNormals, hasTexture)

    companion object {
        fun computeVertexStrideInFloats(hasNormals: Boolean, hasTexture: Boolean): Int {
            var floatStride = 3
            if (hasNormals) floatStride += 3
            if (hasTexture) floatStride += 2
            return floatStride
        }
    }

    val vertexFloatStride = computeVertexStrideInFloats(hasNormals, hasTexture)

    val hasIndexes = indexCount > 0

    init {
        if(indexCount > indexes.size) {
            throw IndexOutOfBoundsException("Index count $indexCount exceeds array size ${indexes.size}")
        }
        if(vertexCount * vertexFloatStride > vertexes.size) {
            throw IndexOutOfBoundsException(
                "Vertex count $vertexCount (*floats per vertex) exceeds array size ${vertexes.size} ")
        }
    }
}
