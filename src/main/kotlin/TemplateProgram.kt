import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.extra.imageFit.FitMethod
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
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
        val videoPlayer1 = VideoPlayerFFMPEG.fromDevice("/dev/video2")
        val videoPlayer2 = VideoPlayerFFMPEG.fromDevice("/dev/video4")
        videoPlayer1.play()
        videoPlayer2.play()

        val canopyWidth = 640
        val canopyHeight = 480

        val imageWidth = canopyWidth - 100
        val imageHeight = canopyHeight - 100
        val upperCanopy = renderTarget(canopyWidth,canopyHeight) {
            colorBuffer()
            depthBuffer()
        }
        val lowerCanopy = renderTarget(canopyWidth,canopyHeight) {
            colorBuffer()
            depthBuffer()
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

        val shadows = renderTarget(imageWidth, imageHeight) {
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

            drawer.isolatedWithTarget(upperCanopy) {
                videoPlayer1.draw(drawer)
            }
            drawer.isolatedWithTarget(lowerCanopy) {
                videoPlayer2.draw(drawer)
            }

            dapple(upperCanopy.colorBuffer(0),lowerCanopy.colorBuffer(0))
            println("$shadows.height" + ", " + "$shadows.width")
            drawer.imageFit(shadows.colorBuffer(0),0.0,0.0,width.toDouble(),height.toDouble(), fitMethod = FitMethod.Cover)
        }
    }
}
