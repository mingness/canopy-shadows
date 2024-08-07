import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.Screenshots
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
        val fern = loadImage("data/images/canopy2.jpg")
        val duckheads = loadImage("data/images/canopy1.jpg")

        val sqWidth = 50
        val mag = 5.0/3.0
        val processWidth = sqWidth*7
        val processMargin = (processWidth - sqWidth)/2
        val canopy1SqBlur = renderTarget(processWidth, processWidth) {
            colorBuffer()
            depthBuffer()
        }
        val blurred = renderTarget(processWidth, processWidth) {
            colorBuffer()
            depthBuffer()
        }
        // -- create blur filter
        val blur = BoxBlur()
        blur.window = 25
        blur.spread = 1.0
        blur.gain = 0.75

        // -- create colorbuffer to hold blur results
        val shadows = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }

        fun dapple(canopy1: ColorBuffer, canopy2: ColorBuffer) {
            val canopyWidth = canopy1.width
            val canopyHeight = canopy1.height
            val cols = canopyWidth/sqWidth
            val rows = canopyHeight/sqWidth

            drawer.isolatedWithTarget(shadows) {
                drawer.clear(ColorRGBa.BLACK)
            }

            for (i in 0 until cols) {
                for (j in 0 until rows) {
                    val source1Rect = IntRectangle(i*sqWidth,j*sqWidth,sqWidth,sqWidth)
                    val target1Rect = IntRectangle(processMargin,processMargin,sqWidth,sqWidth)
                    val source2Rect = Rectangle((i*sqWidth-processMargin).toDouble(),(j*sqWidth-processMargin).toDouble(),processWidth.toDouble(),processWidth.toDouble())
                    val target2Rect = Rectangle(0.0,0.0,processWidth.toDouble(),processWidth.toDouble())

                    drawer.isolatedWithTarget(canopy1SqBlur) {
                        drawer.clear(ColorRGBa.BLACK)
                    }
                    canopy1.copyTo(canopy1SqBlur.colorBuffer(0),0,0,source1Rect,target1Rect)

                    blur.apply(canopy1SqBlur.colorBuffer(0), blurred.colorBuffer(0))

                    drawer.isolatedWithTarget(blurred) {
                        ortho()
                        drawStyle.blendMode = BlendMode.MULTIPLY
                        image(canopy2,source2Rect,target2Rect)
                    }

                    drawer.isolatedWithTarget(shadows) {
                        ortho()
                        drawStyle.blendMode = BlendMode.ADD
                        image(blurred.colorBuffer(0),(i*sqWidth-processMargin).toDouble(),(j*sqWidth-processMargin).toDouble(),processWidth*mag,processWidth*mag)
                    }
                }
            }
        }


        extend(Screenshots())
        extend {
            dapple(fern,duckheads)
            drawer.image(shadows.colorBuffer(0))
        }
    }
}