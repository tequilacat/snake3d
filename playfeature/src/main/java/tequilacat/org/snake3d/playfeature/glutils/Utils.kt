package tequilacat.org.snake3d.playfeature.glutils

import android.content.Context
import java.nio.*
import kotlin.math.*

fun Float.f(digits: Int) = java.lang.String.format("%.${digits}f", this)

const val BYTES_PER_SHORT = 2
const val BYTES_PER_FLOAT = 4

open class Empty {
    //object  Singletons ooo {}
    companion object Default : Empty() {
        val ShortArray = ShortArray(0)
        val FloatArray = FloatArray(0)
    }
}

class CoordUtils {
    companion object {
        /**
         * cross product between p2-p1 and p3-p1
         * @param vertexStride in FLOATS! NOT in bytes
         */
        fun crossProduct(target: FloatArray, targetPos: Int, coords: FloatArray, pos1: Int, pos2: Int, pos3: Int,
                         vertexStride: Int) {
            val p1 = pos1 * vertexStride
            val p2 = pos2 * vertexStride
            val p3 = pos3 * vertexStride

            val uX = coords[p2] - coords[p1]
            val uY = coords[p2 + 1] - coords[p1 + 1]
            val uZ = coords[p2 + 2] - coords[p1 + 2]

            val vX = coords[p3] - coords[p1]
            val vY = coords[p3 + 1] - coords[p1 + 1]
            val vZ = coords[p3 + 2] - coords[p1 + 2]

            target[targetPos] = uY * vZ - uZ * vY
            target[targetPos + 1] = uZ * vX - uX * vZ
            target[targetPos + 2] = uX * vY - uY * vX
            /*
            Nx = UyVz - UzVy
            Ny = UzVx - UxVz
            Nz = UxVy - UyVx*/
        }

        fun distance(p1: FloatArray, p2: FloatArray): Float {
            val dx = p1[0] - p2[0]
            val dy = p1[1] - p2[1]
            val dz = p1[2] - p2[2]

            return Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
        }

        fun length(v: FloatArray, pos: Int = 0) =
            sqrt(v[pos] * v[pos] + v[pos + 1] * v[pos + 1] + v[pos + 2] * v[pos + 2])

        /** src and dst may be same array at same position (but don't overlap!)*/
        fun normalize(dst: FloatArray, dstPos: Int, src: FloatArray, srcPos: Int) {
            val l = 1.0f / length(src, srcPos)
            dst[dstPos] = src[srcPos] * l
            dst[dstPos + 1] = src[srcPos + 1] * l
            dst[dstPos + 2] = src[srcPos + 2] * l
        }
    }
}

// initialize vertex byte buffer for shape coordinates
fun FloatArray.toBuffer(elementCount: Int = -1): FloatBuffer {
    val floatArray: FloatArray = this
    val effCount = if (elementCount >= 0) elementCount else floatArray.size
    // (# of coordinate values * 4 bytes per float)
    return ByteBuffer.allocateDirect(
        effCount * BYTES_PER_FLOAT).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(floatArray, 0, effCount)
                position(0)
            }
        }
}


// initialize byte buffer for the drawGameFrame list
fun ShortArray.toBuffer(elementCount: Int = -1): ShortBuffer {
    val shortArray: ShortArray = this
    val effCount = if (elementCount >= 0) elementCount else shortArray.size
// (# of coordinate values * 2 bytes per short)
    return ByteBuffer.allocateDirect(
        (if (elementCount >= 0) elementCount else shortArray.size) * BYTES_PER_FLOAT).run {
        order(ByteOrder.nativeOrder())
        asShortBuffer().apply {
            put(shortArray, 0, effCount)
            position(0)
        }
    }
}

fun Int.toColorArray() : FloatArray = floatArrayOf(
    ((this ushr 16) and 0xff) / 255f,
    ((this ushr 8) and 0xff) / 255f,
    (this and 0xff) / 255f,
    1f)


/*
fun loadAsset() {
    try {
    val json_string = application.assets.open(file_name).bufferedReader().use{
        it.readText()
    }

        val inputStream = assets.open("news_data_file.json")
        val inputString = inputStream.bufferedReader().use{it.readText()}
        Log.d("res",inputString)
    } catch (e:Exception){
        Log.d("res", e.toString())
    }
}*/

fun readResourceText(context: Context, resourceId: Int): String {
    return context.resources.openRawResource(resourceId).use {
            r -> r.readBytes().toString(Charsets.UTF_8)
    }
}