import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.*

fun main() = application {
    configure {
//        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
//        hideCursor = true
        width = 1400
        height = 1000
    }
    program {
        val canopy1 = loadImage("data/images/fern.jpg")
        val canopy2 = loadImage("data/images/duckheads.jpg")

        val imageWidth = canopy1.width
        val imageHeight = canopy1.height

        val imageRect = Rectangle(0.0,0.0,imageWidth.toDouble(),imageHeight.toDouble())
        val shrunkRect = Rectangle(0.0,0.0,imageWidth.toDouble()/10,imageHeight.toDouble()/10)

        var x = 0.0
        var y = 0.0
        val mag = 5.0/3.0

        val rotated = renderTarget(canopy1.height, canopy1.width) {
            colorBuffer()
            depthBuffer()
        }
        val blurred = renderTarget(canopy1.height, canopy1.width) {
            colorBuffer()
            depthBuffer()
        }
        // -- create blur filter
        val blur10 = BoxBlur()
        blur10.window = 10
        blur10.spread = 1.0

        val blur5 = BoxBlur()
        blur5.window = 5
        blur5.spread = 1.0

        val shrunk = renderTarget(canopy1.height/10, canopy1.width/10) {
            colorBuffer()
            depthBuffer()
        }

        val shadows = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }


        extend(Screenshots())
        extend {
//            x = 0.0
//            y = 0.0
//            var target = Rectangle(x,y,imageWidth.toDouble(), imageHeight.toDouble())
//            drawer.image(fern, imageRect, target)
//
//            blur10.apply(fern, blurred.colorBuffer(0))
//            x = x + imageWidth.toDouble()
//            target = Rectangle(x,y,imageWidth.toDouble(), imageHeight.toDouble())
//            drawer.image(blurred.colorBuffer(0), imageRect, target)
//
//            drawer.isolatedWithTarget(shrunk) {
//                ortho(shrunk)
//                image(fern,imageRect,shrunkRect)
//            }
//            x = x + imageWidth.toDouble()
//            target = Rectangle(x,y,imageWidth.toDouble(), imageHeight.toDouble())
//            drawer.image(shrunk.colorBuffer(0), shrunkRect, target)

            drawer.isolatedWithTarget(rotated) {
                ortho(rotated)
                clear(ColorRGBa.WHITE)
                translate(width/2.0, height/2.0)
                rotate(seconds * 30.0)
                image(canopy1,-width/2.0, -height/2.0)
            }
            val animation = rotated.colorBuffer(0)
            animation.shadow.download()
            blur5.apply(animation, blurred.colorBuffer(0))
            var blrrd = blurred.colorBuffer(0)
            blrrd.shadow.download()

            drawer.clear(ColorRGBa.BLACK)
            drawer.stroke = ColorRGBa.RED
            drawer.fill = null
            fun f(v: Vector2): Double {
                val iv = v.toInt()
//                val d = if (iv.x >= 0 && iv.y >= 0 && iv.x < fern.width && iv.y < fern.height) fern.shadow[iv.x, iv.y].luminance else 0.0
                val d = if (iv.x >= 0 && iv.y >= 0 && iv.x < blurred.width && iv.y < blurred.height) blrrd.shadow[iv.x, iv.y].luminance else 0.0
                return cos(d * PI)
            }

            val contours = findContours(::f, drawer.bounds.offsetEdges(32.0), 4.0)
            drawer.drawStyle.colorMatrix = grayscale()
            drawer.image(blrrd)
            drawer.contours(contours)

        }
    }
}
