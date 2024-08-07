import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.extra.imageFit.FitMethod
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.shape.IntRectangle
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
        val fern = loadImage("data/images/canopy2.jpg")
        val duckheads = loadImage("data/images/canopy1.jpg")

        val imageWidth = fern.width
        val imageHeight = fern.height

        val imageRect = Rectangle(0.0,0.0,imageWidth.toDouble(),imageHeight.toDouble())
        val shrunkRect = Rectangle(0.0,0.0,imageWidth.toDouble()/10,imageHeight.toDouble()/10)

        var x = 0.0
        var y = 0.0
        val mag = 5.0/3.0

        val rotated = renderTarget(fern.height, fern.width) {
            colorBuffer()
            depthBuffer()
        }
        val blurred = renderTarget(fern.height, fern.width) {
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

        val shrunk = renderTarget(fern.height/10, fern.width/10) {
            colorBuffer()
            depthBuffer()
        }

        val shadows = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }


        extend(Screenshots())
        extend {
            x = 0.0
            y = 0.0
            var target = Rectangle(x,y,imageWidth.toDouble(), imageHeight.toDouble())
            drawer.image(fern, imageRect, target)

            blur10.apply(fern, blurred.colorBuffer(0))
            x = x + imageWidth.toDouble()
            target = Rectangle(x,y,imageWidth.toDouble(), imageHeight.toDouble())
            drawer.image(blurred.colorBuffer(0), imageRect, target)

            drawer.isolatedWithTarget(shrunk) {
                ortho(shrunk)
                image(fern,imageRect,shrunkRect)
            }
            x = x + imageWidth.toDouble()
            target = Rectangle(x,y,imageWidth.toDouble(), imageHeight.toDouble())
            drawer.image(shrunk.colorBuffer(0), shrunkRect, target)

            drawer.isolatedWithTarget(rotated) {
                ortho(rotated)
                clear(ColorRGBa.WHITE)
                translate(width/2.0, height/2.0)
                rotate(seconds * 30.0)
                image(fern,-width/2.0, -height/2.0)
            }
            val animation = rotated.colorBuffer(0)
            x = 0.0
            y = y + imageHeight.toDouble()
            target = Rectangle(x,y,imageWidth.toDouble(), imageHeight.toDouble())
            drawer.image(animation, imageRect, target)

            blur5.apply(animation, blurred.colorBuffer(0))
            var blrrd = blurred.colorBuffer(0)
            x = x + imageWidth.toDouble()
            target = Rectangle(x,y,imageWidth.toDouble(), imageHeight.toDouble())
            drawer.image(blrrd, imageRect, target)

        }
    }
}
