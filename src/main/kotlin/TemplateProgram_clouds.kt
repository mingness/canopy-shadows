import org.openrndr.Display
import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.imageFit.FitMethod
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.noise.perlinLinear
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector4
import org.openrndr.panel.style.Position
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

fun main() = application {
    configure {
//        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        width = 1200
        height = 900
    }
    program {
        val canopyWidth = 400
        val canopyHeight = 300
        val movingCircles1 = renderTarget(canopyWidth,canopyHeight) {
            colorBuffer()
            depthBuffer()
        }

        extend(Screenshots())
        extend {
            drawer.isolatedWithTarget(movingCircles1) {
                ortho()
                drawer.clear(ColorRGBa.WHITE)
                drawer.stroke = null
                drawer.drawStyle.blendMode = BlendMode.SUBTRACT
                val scale = 0.05
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val noise = perlinLinear(100, x * scale-seconds, y * scale)
                        drawer.fill = ColorRGBa(0.5,0.5,0.5,noise*0.2)
                        drawer.circle(x * 1.0, y * 1.0, 10.0)
                    }
                }
            }

            drawer.image(movingCircles1.colorBuffer(0))
        }
    }
}