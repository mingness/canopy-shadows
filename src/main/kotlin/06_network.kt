import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.drawImage
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.math.times
import org.openrndr.shape.Rectangle
import kotlin.math.*

fun main() = application {
    configure {
//        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
//        hideCursor = true
        width = 600
        height = 600
    }
    program {
        val canopy1 = loadImage("data/images/fern.jpg")
        val canopy2 = loadImage("data/images/duckheads.jpg")


        val imageWidth = canopy1.width
        val imageHeight = canopy1.height

        val withNetwork = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }

        val numNodes = 5
        val nodeParameters = List(numNodes) {
            Vector4.uniform(0.0,1.0)
        }
        val nodeIndexList = mutableListOf<Int>()
        for (i in 0 until numNodes) {
            for (j in 0 until  i) {
                nodeIndexList += i
                nodeIndexList += j
            }
        }


        extend(Screenshots())
        extend {
            // simulate video
            drawer.isolatedWithTarget(withNetwork) {
                ortho(withNetwork)
                clear(ColorRGBa.WHITE)
                image(canopy1)
                val nodePoints = mutableListOf<Vector2>()
                for (params in nodeParameters) {
                    nodePoints += Vector2(imageWidth*(params[0] +0.2*cos(seconds*params[2]/10.0 + params[3])),
                        imageHeight*(params[1] + 0.2*sin(seconds*params[3]/10.0 + params[2])))
                }
                val nodeList = nodeIndexList.map { nodePoints[it]}
                stroke = ColorRGBa.BLACK
                strokeWeight = 4.0
                lineSegments(nodeList)
            }
            val animation = withNetwork.colorBuffer(0)
            drawer.image(animation)
        }
    }
}


