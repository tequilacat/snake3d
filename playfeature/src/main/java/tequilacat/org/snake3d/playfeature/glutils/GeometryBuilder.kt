package tequilacat.org.snake3d.playfeature.glutils

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class GeometryBuilder {

    /**
     * makes rotation, smooth or facetted vertically. if 2 points repeat it's facette between them.
     */
//    fun makeRotation(points2d: FloatArray, from: Int, pointCount: Int, sides: Int): GeometryData {
//        // find sizes of vertexes by computing hard breaks (equal points).
//        // if y = 0 use common vertex
//
//        val lastX = 0f
//        val lastY = 0f
//
//        for(pIndex in from until from + pointCount * 2 step 2){
//            val x = points2d[pIndex]
//            val y = points2d[pIndex + 1]
//
//            if (pIndex > from && lastX == x && lastY == y) {
//                // same point - assign different vertexes
//            }
//
//            // use single vertex (facette)
//            if(y == 0f) {
//
//            }
//        }
//    }

    fun makePrism(
        cx: Float, cy: Float, zBase: Float,
        height: Float, radius: Float, sides: Int,
        addCap: Boolean,
        addTextures: Boolean
    ): GeometryData {
        val vertexes = makePrismVertexes(
            cx,
            cy,
            radius,
            sides,
            zBase,
            height,
            addTextures
        )
        val indexes = makePrismIndexes(sides, addCap)
        return makeFacettedGeometryData(vertexes, indexes, addTextures)
    }

    private fun makePrismVertexes(
        cx: Float, cy: Float, radius: Float, sides: Int,
        zBase: Float, height: Float, addTextureUV: Boolean
    ): FloatArray {
        val deltaAlpha: Float = PI.toFloat() * 2 / sides.toFloat()
        val coords = FloatArray(sides * 2 * computeVertexFloatStride(addTextureUV))
        var pos = 0
        var alpha = 0f
        var t = 0f
        val dt = 1f / sides

        for (side in 0 until sides) {
            val x = cx + radius * cos(alpha)
            val y = cy + radius * sin(alpha)

            coords[pos++] = x
            coords[pos++] = y
            coords[pos++] = zBase

            if(addTextureUV) {
                coords[pos++] = t
                coords[pos++] = 1f
            }

            coords[pos++] = x
            coords[pos++] = y
            coords[pos++] = zBase + height

            if(addTextureUV) {
                coords[pos++] = t
                coords[pos++] = 0f
            }

            t += dt
            alpha += deltaAlpha
        }

        return coords
    }

    companion object {
        private val RECT_VERTEX_ORDER_LIST = shortArrayOf(0, 1, 2, 0, 2, 3)

        private fun computeVertexFloatStride(textures: Boolean) = if(textures) 5 else 3

        /** used by algos as output to build normal. NOT threadsafe! */
        private val normalTmpBuffer = FloatArray(3)

        /**
         * creates prism indexes for specified sides count,
         * assume prism along 0Z, vertexes follow around center counter-clockwise (increasing alpha)
         */
        private fun makePrismIndexes(sides: Int, addCap: Boolean) : ShortArray {
            val array = ShortArray(sides * 6 + if (addCap) (sides - 2) * 3 else 0)
            var pos = 0

            for (sideVertex in 0 until sides) {
                val p0: Short = (sideVertex * 2).toShort()
                val p1: Short = (p0 + 1).toShort()
                val p2: Short = if (sideVertex < sides - 1) (p0 + 2).toShort() else 0
                val p3: Short = (p2 + 1).toShort()
                array[pos++] = p0
                array[pos++] = p2
                array[pos++] = p1
                array[pos++] = p2
                array[pos++] = p3
                array[pos++] = p1
            }

            if (addCap) {
                val first: Short = 1
                var next: Short = 3 // first + 1

                for(i in 0 until sides - 2) {
                    array[pos++] = first
                    array[pos++] = next
                    next = (next + 2).toShort()
                    array[pos++] = next
                }
            }
            return array
        }

        /**
         * generates separate vertices and a normal for each vertex, without indexing
         */
        fun makeFacettedGeometryData(
            vertexes: FloatArray,
            indexes: ShortArray,
            textures: Boolean
        ): GeometryData {

            val vertexFloatStride = computeVertexFloatStride(textures)

            // make target vertexes
            // no reuse of vertexes - facets will be visible
            val resVertexCount = indexes.size // for each index there will be a vertex
            val outVertexes = FloatArray(resVertexCount * (3 + vertexFloatStride)) // stride + 3 per normal
            // val normals = FloatArray(resVertexCount * 3)
            // val singleNormalCoords = FloatArray(3) // temp out array
            var coordPtr = 0 //

            for (i in 0 until indexes.size step 3) {
                // for all 3 face vertices the normal is same so compute once
                CoordUtils.crossProduct(
                    normalTmpBuffer, 0,
                    vertexes, indexes[i], indexes[i + 1], indexes[i + 2], vertexFloatStride
                )
                CoordUtils.normalize(
                    normalTmpBuffer,
                    0,
                    normalTmpBuffer,
                    0
                )

                for(vI in 0 .. 2) {
                    val cPos = indexes[i + vI] * vertexFloatStride // position of coords

                    for(vPos in 0 until vertexFloatStride) {
                        outVertexes[coordPtr++] = vertexes[cPos + vPos]
                        //outVertexes[coordPtr++] = vertexes[cPos + 1]
                        //outVertexes[coordPtr++] = vertexes[cPos + 2]
                    }

                    outVertexes[coordPtr++] = normalTmpBuffer[0]
                    outVertexes[coordPtr++] = normalTmpBuffer[1]
                    outVertexes[coordPtr++] = normalTmpBuffer[2]
                }
            }

            return GeometryData(outVertexes, true, textures)
        }
    }

    private val storedVertexes = mutableListOf<Float>()
    private val storedIndexes = mutableListOf<Short>()
    private var freeIndex: Short = 0

    fun add(vertexes: FloatArray, indexes: ShortArray) {
        vertexes.forEach { storedVertexes.add(it) }
        val currentBase = freeIndex

        indexes.forEach {
            val index : Short = (currentBase + it).toShort()
            if (freeIndex < index) {
                freeIndex = index
            }
            storedIndexes.add(index)
        }
        freeIndex++
    }

    var textureUvAdded = false

    fun addQuad(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        textureUV: FloatArray = Empty.FloatArray
    ) {
        // currently all added quads must be added exactly with same param
        // otherwise result will be inconsistent
        textureUvAdded = textureUV.isNotEmpty()

        val hasUV = textureUV.size > 0
        // check stride
        val uvOffset = 3
        val normalOffset = if (hasUV) uvOffset + 2 else uvOffset

        var stride = 6 // always add normals
        if (hasUV) stride += 2
        val newArray = FloatArray(stride * 4)

        // copy one by one
        var outPos = 0
        newArray[outPos] = x0
        newArray[outPos + 1] = y0
        newArray[outPos + 2] = z0

        outPos += stride
        newArray[outPos] = x1
        newArray[outPos + 1] = y1
        newArray[outPos + 2] = z1

        outPos += stride
        newArray[outPos] = x2
        newArray[outPos + 1] = y2
        newArray[outPos + 2] = z2

        outPos += stride
        newArray[outPos] = x3
        newArray[outPos + 1] = y3
        newArray[outPos + 2] = z3

        var uvPos = uvOffset
        var normalPos = normalOffset

        for (i in 0 until 4) {
            if(hasUV) {
                if (i == 0) {
                    newArray[uvPos] = textureUV[0]
                    newArray[uvPos+1] = textureUV[1]
                } else if (i == 1) {
                    newArray[uvPos] = textureUV[2]
                    newArray[uvPos+1] = textureUV[1]
                } else if (i == 2) {
                    newArray[uvPos] = textureUV[2]
                    newArray[uvPos + 1] = textureUV[3]
                } else { // i == 3
                    newArray[uvPos] = textureUV[0]
                    newArray[uvPos + 1] = textureUV[3]
                }
            }

            if(i == 0) {
                // compute normal
                CoordUtils.crossProduct(
                    newArray, normalPos,
                    newArray, 0, 1, 2,
                    stride /* float stride always 3 - use 3 first generated floats as coords */
                )
                CoordUtils.normalize(
                    newArray,
                    normalPos,
                    newArray,
                    normalPos
                )
            } else {
                newArray.copyInto(newArray, normalPos, normalOffset, normalOffset + 3) // copy 3
            }

            uvPos += stride
            normalPos += stride
        }

        add(
            newArray,
            //floatArrayOf(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3),
            RECT_VERTEX_ORDER_LIST
        )
    }

//    fun build() = makeFacettedGeometryData(storedVertexes.toFloatArray(),
//        storedIndexes.toShortArray(), addTextures)

    fun build() = GeometryData(
        storedVertexes.toFloatArray(), true, textureUvAdded,
        storedIndexes.toShortArray()
    )
}