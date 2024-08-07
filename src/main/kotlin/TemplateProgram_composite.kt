import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.blend.Normal
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle

fun main() = application {
    configure {
        width = 600
        height = 600
    }
    program {
        val sqWidth = 50
        val mag = 5.0/3.0
        val cols = 300/sqWidth
        val processWidth = sqWidth*7
        val processMargin = (processWidth - sqWidth)/2
        val shadows = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }

        val composite = compose {
            layer {
//                blend(Add()) {
//                    clip = true
//                }
                val canopy2 = loadImage("data/images/canopy1.jpg")
                draw {
                    drawer.clear(ColorRGBa.BLACK)
                    drawer.image(canopy2)
                }
            }
            layer {
                val canopy1 = loadImage("data/images/canopy2.jpg")
                blend(Add())
                draw {
                    drawer.image(canopy1)
                }
                post(ApproximateGaussianBlur()) {
                    // -- this is actually a function that is called for every draw
                    window = 25
                    sigma = 10.01
                }
            }
        }

        extend(Screenshots())
        extend {
            drawer.isolatedWithTarget(shadows) {
                composite.draw(drawer)
            }
            drawer.drawStyle.blendMode = BlendMode.ADD
            val areas = (0..10).flatMap { y ->
                (0..10).map { x ->
                    val source = Rectangle(x * (width / 10.0), y * (height / 10.0), width / 5.0, height / 5.0)
                    val target = Rectangle(x * (width / 10.0), y * (height / 10.0), width / 10.0, height / 10.0)
                    source to target
                }
            }
                drawer.image(shadows.colorBuffer(0), areas)

        }
    }
}