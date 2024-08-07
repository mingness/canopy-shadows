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
        val movingCircles2 = renderTarget(canopyWidth,canopyHeight) {
            colorBuffer()
            depthBuffer()
        }

        val circleRadius = 20.0

        val circleParameters1 = List(24) {
            Vector4(random(0.2,0.8),random(0.2,0.8),random(0.0,1.0),random(0.0,1.0))
        }
        val circleParameters2 = List(24) {
            Vector4(random(0.2,0.8),random(0.2,0.8),random(0.0,1.0),random(0.0,1.0))
        }

        val sqWidth = 30
        val magR = 5.0/3.0
        val magG = 5.0/3.0+0.1
        val magB = 5.0/3.0+0.2
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
//        val blur = ApproximateGaussianBlur()
        blur.window = 15
        blur.spread = 1.0
        blur.gain = 0.75

        val aberration = ChromaticAberration()
        aberration.aberrationFactor = -2.0

        // -- create colorbuffer to hold blur results
        val shadows = renderTarget(canopyWidth, canopyHeight) {
            colorBuffer()
            depthBuffer()
        }
        val aberred = renderTarget(canopyWidth, canopyHeight) {
            colorBuffer()
            depthBuffer()
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
//                    canopy1SqBlur.colorBuffer(0).copyTo(blurred.colorBuffer(0))

                    drawer.isolatedWithTarget(blurred) {
                        ortho()
                        drawStyle.blendMode = BlendMode.MULTIPLY
                        image(canopy2,source2Rect,target2Rect)
                    }

                    drawer.isolatedWithTarget(shadows) {
                        ortho()
                        drawStyle.blendMode = BlendMode.ADD
                        drawer.drawStyle.colorMatrix = tint(ColorRGBa.RED)
                        image(blurred.colorBuffer(0),(i*sqWidth-processMargin*magR),(j*sqWidth-processMargin*magR),processWidth*magR,processWidth*magR)
                        drawer.drawStyle.colorMatrix = tint(ColorRGBa.GREEN)
                        image(blurred.colorBuffer(0),(i*sqWidth-processMargin*magG),(j*sqWidth-processMargin*magG),processWidth*magG,processWidth*magG)
                        drawer.drawStyle.colorMatrix = tint(ColorRGBa.BLUE)
                        image(blurred.colorBuffer(0),(i*sqWidth-processMargin*magB),(j*sqWidth-processMargin*magB),processWidth*magB,processWidth*magB)
                    }
                    aberration.apply(shadows.colorBuffer(0),aberred.colorBuffer(0))
                }
            }
        }


        extend(Screenshots())
        extend {
            drawer.isolatedWithTarget(movingCircles1) {
                ortho()
                drawer.clear(ColorRGBa.WHITE)
                drawer.fill = ColorRGBa.BLACK
                drawer.stroke = null
                for (params in circleParameters1) {
                    drawer.circle(canopyWidth*(params[0] +0.2*cos(seconds*params[2] + params[3])),
                        canopyHeight*(params[1] + 0.2*sin(seconds*params[3] + params[2])),
                        circleRadius)
                }
            }

            drawer.isolatedWithTarget(movingCircles2) {
                ortho()
                drawer.clear(ColorRGBa.WHITE)
                drawer.fill = ColorRGBa.BLACK
                drawer.stroke = null
                for (params in circleParameters2) {
                    drawer.circle(canopyWidth*(params[0] +0.2*cos(seconds*params[2] + params[3])),
                        canopyHeight*(params[1] + 0.2*sin(seconds*params[3] + params[2])),
                        circleRadius)
                }
            }

//            drawer.image(movingCircles1.colorBuffer(0))
            dapple(movingCircles1.colorBuffer(0),movingCircles2.colorBuffer(0))
//            drawer.imageFit(shadows.colorBuffer(0),0.0,0.0,width.toDouble(),height.toDouble(), fitMethod = FitMethod.Contain)
            drawer.imageFit(aberred.colorBuffer(0),0.0,0.0,width.toDouble(),height.toDouble(), fitMethod = FitMethod.Contain)
        }
    }
}