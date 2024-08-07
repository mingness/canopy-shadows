import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.shape.IntRectangle

fun main() = application {
    configure {
        width = 600
        height = 600
    }
    program {
        val canopy1 = loadImage("data/images/canopy2.jpg")
        val canopy2 = loadImage("data/images/canopy1.jpg")

        val mag = 5.0/3.0
        val sqWidth = 30
        val cols = 300/sqWidth
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

        for (i in 0 until cols) {
            for (j in 0 until cols) {
                val canopy1Sq = canopy1.crop(IntRectangle(i*sqWidth,j*sqWidth,sqWidth,sqWidth))
                val canopy2Sq = canopy2.crop(IntRectangle(i*sqWidth-processMargin,j*sqWidth-processMargin,processWidth,processWidth))

                drawer.isolatedWithTarget(canopy1SqBlur) {
                    ortho()
                    drawStyle.blendMode = BlendMode.ADD
                    clear(ColorRGBa.BLACK)
                    image(canopy1Sq,processMargin.toDouble(),processMargin.toDouble())
                }
                blur.apply(canopy1SqBlur.colorBuffer(0), blurred.colorBuffer(0))

                drawer.isolatedWithTarget(blurred) {
                    ortho()
                    drawStyle.blendMode = BlendMode.MULTIPLY
                    image(canopy2Sq,0.0,0.0)
                }

                drawer.isolatedWithTarget(shadows) {
                    ortho()
                    drawStyle.blendMode = BlendMode.ADD
                    image(blurred.colorBuffer(0),(i*sqWidth-processMargin).toDouble(),(j*sqWidth-processMargin).toDouble(),processWidth*mag,processWidth*mag)
                }
            }
        }

        extend(Screenshots())
        extend {
            drawer.image(shadows.colorBuffer(0))
        }

//        extend {
//            drawer.shadeStyle = shadeStyle {
//                fragmentPreamble = """
//          const float PI = 3.1415926538;
//          const float PI_2 = PI * 2.;
//          const float PI_HALF = PI / 2.;
//          const float ANTIALIASING = .02;
//          const float ROTATION_SPEED = .5;
//
//          float getEdge(vec2 coord, float segments, float extraAngle) {
//              float angle = atan(coord.x, coord.y) - PI_HALF;
//              angle += extraAngle; //mod(iTime * .2, PI_2);
//              float dist = length(coord);
//              float segmentFactor = .1 * (sin(p_seconds * .3) + 1.);
//              return
//                  dist
//                  + (sin(angle) + 1.) * .5
//                  + (cos(angle * segments) + 1.) * segmentFactor;
//          }
//        """.trimIndent()
//                fragmentTransform = """
//          vec2 coord = (c_boundsPosition.xy - vec2(.5)) * 2.0;
//          float distanceGradient = getEdge(coord, 3.0, p_seconds);
//          float luma = smoothstep(1.0, 0.0, distanceGradient);
//          x_fill = vec4(luma);
//        """.trimIndent()
//                parameter("seconds", seconds)
//            }
//            drawer.drawStyle.blendMode = BlendMode.ADD
//        }
    }
}