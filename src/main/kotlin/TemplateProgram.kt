import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.imageFit.FitMethod
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector4
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import kotlin.math.*

fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        hideCursor = true
        width = 1400
        height = 1000
    }
    program {
        val leaf = shadeStyle {
            fragmentPreamble = """
          const float PI = 3.1415926538;
          const float PI_2 = PI * 2.;
          const float PI_HALF = PI / 2.;
          
          float leafSdf(in vec2 coord) {
              float angle = atan(coord.x, coord.y) + PI_HALF + p_rotation;
              float dist = length(coord);
              return
                  dist
                  + (sin(angle) + 1.) * .5
                  + (cos(angle * p_segments) + 1.) * p_segmentFactor;    
          }
        """.trimIndent()
            fragmentTransform = """
          vec2 st = (c_boundsPosition.xy - vec2(0.5)) * 2.0;
          float sdf = leafSdf(st);
          float luma = smoothstep(p_smoothing, 1.0, sdf);
          x_fill.a = 1.0-luma;
        """.trimIndent()
        }

        val canopyWidth = 400
        val canopyHeight = 300
        val imageWidth = canopyWidth + 50
        val imageHeight = canopyHeight + 50
        val movingPersons = renderTarget(canopyWidth,canopyHeight) {
            colorBuffer()
            depthBuffer()
        }
        val movingCircles2 = renderTarget(canopyWidth,canopyHeight) {
            colorBuffer()
            depthBuffer()
        }

        val headRadius = 10
        val bodyWidth = 30
        val bodyWobble = 2
        val leafRadius = 40.0

        val personParameters1 = List(10) {
            Vector4.uniform(0.0,1.0)
        }
        val circleParameters2 = List(36) {
            Vector4.uniform(0.0,1.0)
        }

        val sqWidth = 30
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
        blur.window = 15
        blur.spread = 1.0
        blur.gain = 0.5

        val aberration = ChromaticAberration()
        aberration.aberrationFactor = -2.0

        val shadows = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }
        val aberred = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }

        val period = 5.0*60.0
        fun mainEnvelope(time: Double): Double {
            return min(1.0, 2.0*abs(sin(PI*time/period)))
        }

        fun dapple(canopy1: ColorBuffer, canopy2: ColorBuffer) {
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
                        image(blurred.colorBuffer(0),(i*sqWidth-processMargin*mag+20),(j*sqWidth-processMargin*mag+10),processWidth*mag,processWidth*mag)
                    }
                }
            }
        }


        extend(Screenshots())
        extend {
            drawer.stroke = null

            drawer.isolatedWithTarget(movingPersons) {
                ortho()
                drawer.clear(ColorRGBa.WHITE)
                drawer.fill = ColorRGBa.BLACK

                for (params in personParameters1) {
                    val thisX = canopyWidth*(params[0] + cos(seconds*params[2]/10.0 + 6.0*params[3]))
                    val thisY = canopyHeight*(0.4+0.05*params[1])
                    drawer.stroke = null
                    drawer.circle(thisX,
                        thisY,
                        headRadius.toDouble())
                    drawer.rectangle(thisX-bodyWidth/2+bodyWobble*sin(seconds*0.1+params[2]),
                        thisY+headRadius*1.5,
                        bodyWidth.toDouble(),canopyHeight*0.25)
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.strokeWeight = 10.0
                    drawer.lineSegment(thisX,
                        thisY+headRadius*1.5+canopyHeight*0.25,
                        thisX + bodyWidth*sin(seconds*params[2] + params[3]),thisY+headRadius*1.5+canopyHeight*0.5)
                    drawer.lineSegment(thisX,
                        thisY+headRadius*1.5+canopyHeight*0.25,
                        thisX - bodyWidth*sin(seconds*params[2] + params[3]),thisY+headRadius*1.5+canopyHeight*0.5)
                }
            }

            drawer.isolatedWithTarget(movingCircles2) {
                ortho()
                drawer.clear(ColorRGBa.WHITE)
                drawer.fill = ColorRGBa.BLACK
                drawer.stroke = null

                leaf.parameter("seconds", seconds/2.0)
                leaf.parameter("segments", 10)
                leaf.parameter("segmentFactor", 0.1)
                leaf.parameter("smoothing", 0.8)
                drawer.shadeStyle = leaf
                for (params in circleParameters2) {
                    leaf.parameter("rotation", params[2] + seconds*params[3])
                    val x = canopyWidth*(params[0] +0.2*cos(seconds*params[2]/2.0 + params[3]))
                    val y = mainEnvelope(seconds)*(canopyHeight*(params[1] + 0.2*sin(seconds*params[3]/2.0 + params[2]))) +
                            (1 - mainEnvelope(seconds))*canopyHeight
                    drawer.circle(x, y, leafRadius)
                }
            }

//            drawer.image(movingPersons.colorBuffer(0))
//            drawer.image(movingCircles2.colorBuffer(0))
            dapple(movingPersons.colorBuffer(0),movingCircles2.colorBuffer(0))
            aberration.apply(shadows.colorBuffer(0),aberred.colorBuffer(0))
            drawer.imageFit(aberred.colorBuffer(0),0.0,0.0,width.toDouble(),height.toDouble(), fitMethod = FitMethod.Contain)
        }
    }
}
