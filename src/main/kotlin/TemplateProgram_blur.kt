import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blur.*
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.writer
import kotlin.math.floor
import kotlin.math.sin

fun main() = application {
    configure {
        width = 600
        height = 600
    }
    program {
        // -- create blur filter
        val blur = BoxBlur()
//        blur.window = 16 // 1 to 25
//        blur.spread = 1.0 // 1 to 4
//        blur.gain = 0.75 // 0 to 4

//        val blur = ApproximateGaussianBlur() // when blur too much, looks boxy
//        blur.window = 16 // 1 to 25
//        blur.sigma = 25.0 // 0 to 25
//        blur.gain = 0.75 // 0 to 4

//        val blur = GaussianBlur() // somehow not accurate, still boxy

//        val blur = HashBlur() // exploding speckle blur
//        blur.radius = 5.0 // 1 to 25
//        blur.samples = 100 // 1 to 100

        // -- create colorbuffer to hold blur results
        val circle = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }
        val blurred = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }

        drawer.isolatedWithTarget(circle) {
            clear(ColorRGBa.BLACK)
            fill = ColorRGBa.WHITE
            stroke = null
//                circle(width/2.0, height/2.0, 100*(1+sin(seconds)))
            circle(width/4.0, height*0.75, 16.0)
            circle(width/4.0, height*0.25, 8.0)
            circle(width*0.75, height*0.25, 4.0)
            circle(width*0.75, height*0.5, 2.0)
            circle(width*0.75, height*0.75, 1.0)
            circle(width*0.5, height*0.75, 0.5)
        }


        extend(Screenshots())
        extend {
//            val window = 16.0// + 24*sin(seconds/4)*sin(seconds/4) // 1 to 25
//            val spread = 1.0+3*sin(seconds/2)*sin(seconds/2) // 1 to 4
//            val gain = 4.0//4.0*sin(seconds)*sin(seconds) // 0 to 4
//            val sigma = 25.0//*sin(seconds/2)*sin(seconds/2) // 0 to 25
//            println("window = $window, ")
//            println("spread = $spread, ")
//            println("sigma = $sigma, ")
//            println("gain = $gain")

//            blur.window = window.toInt()
//            blur.spread = spread
//            blur.sigma = sigma
//            blur.gain = gain

            //Hashblur
//            val radius = 1.0 + 24*sin(seconds/4)*sin(seconds/4) // 1 to 25
//            val samples = 100//1 + 99*sin(seconds/4)*sin(seconds/4) // 1 to 25
//            val gain = 4.0//1 + 99*sin(seconds/4)*sin(seconds/4) // 1 to 25
//            println("radius = $radius")
//            println("samples = $samples")
//            blur.radius = radius
//            blur.samples = samples.toInt()
//            blur.gain = gain

            blur.apply(circle.colorBuffer(0), blurred.colorBuffer(0))
            drawer.image(blurred.colorBuffer(0))
        }
    }
}