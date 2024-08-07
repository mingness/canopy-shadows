import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.color.presets.DARK_GRAY
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.math.Vector3
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle

fun main() = application {
    configure {
        width = 600
        height = 600
    }
    program {
        val canopy1 = loadImage("data/images/canopy2.jpg")
        val canopy2 = loadImage("data/images/canopy1.jpg")

        val shadow = canopy1.shadow
        shadow.download()

        val blurRadius = 10.0
        val mag = 5.0/3.0
        val processWidth = blurRadius.toInt()*2
        val masked = renderTarget(processWidth, processWidth) {
            colorBuffer()
            depthBuffer()
        }

//        val geometry = vertexBuffer(vertexFormat {
//            position(3)
//        }, 300 * 300)
//
//        geometry.put {
//            for (i in 0 until geometry.vertexCount) {
//                write(Vector3((i % 300).toDouble(), (i/300).toDouble(), 0.0))
//            }
//        }

        extend(Screenshots())
        extend {
            drawer.clear(ColorRGBa.BLACK)
//            drawer.vertexBuffer(geometry, DrawPrimitive.POINTS)
            drawer.drawStyle.blendMode = BlendMode.ADD

            for (y in 0 until canopy1.height) {
                for (x in 0 until canopy1.width) {
                    if (shadow[x,y].r > 0) {
                        drawer.isolatedWithTarget(masked) {
                            clear(ColorRGBa.BLACK)
                            fill = ColorRGBa(0.005, 0.005, 0.005)
                            stroke = null
                            drawStyle.blendMode = BlendMode.ADD
                            circle(blurRadius,blurRadius,blurRadius)

//                            drawStyle.blendMode = BlendMode.MULTIPLY
//                            val source = Rectangle(x-blurRadius,y-blurRadius,blurRadius*2.0,blurRadius*2.0)
//                            val target = Rectangle(0.0,0.0,blurRadius*2.0,blurRadius*2.0)
//                            image(canopy2, source, target)
                        }
                        drawer.image(masked.colorBuffer(0),x*1.0,y*1.0,blurRadius)
                    }
                }
            }

        }
    }
}