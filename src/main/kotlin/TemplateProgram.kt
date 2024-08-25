import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.drawImage
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.color.presets.DARK_GRAY
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.extra.noise.uniform
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.math.times
import org.openrndr.shape.Rectangle
import kotlin.math.*

fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        hideCursor = true
//        width = 1400
//        height = 1000
    }
    program {
        val bokehThreshold = 5000
        val bokehRadius = 50.0
        val bokehWidth = bokehRadius*2
        val mag = 5.0 / 3.0
        val grayLevel = 0.1

        val videoPlayer1 = VideoPlayerFFMPEG.fromDevice("/dev/video2")
        val videoPlayer2 = VideoPlayerFFMPEG.fromDevice("/dev/video0"   )
        videoPlayer1.play()
        videoPlayer2.play()

        val canopyWidth = 640
        val canopyHeight = 480

        val margin = 50
        val imageWidth = canopyWidth - margin*2
        val imageHeight = canopyHeight - margin*2

        val upperCanopy = renderTarget(canopyWidth, canopyHeight) {
            colorBuffer()
            depthBuffer()
        }
        val lowerCanopy = renderTarget(canopyWidth, canopyHeight) {
            colorBuffer()
            depthBuffer()
        }
        val blurredCanopy2 = renderTarget(canopyWidth, canopyHeight) {
            colorBuffer()
            depthBuffer()
        }

        val preppedCanopy1 = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }
        val preppedCanopy2 = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }
        val blurred = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }
        val shadows = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }
        val blurredFinal = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }

        val bokeh = drawImage(bokehWidth.toInt(), bokehWidth.toInt()) {
            drawer.clear(ColorRGBa.BLACK)
            drawer.fill = ColorRGBa.DARK_GRAY
            drawer.circle(bokehRadius,bokehRadius,bokehRadius)
        }
        val masked = renderTarget(bokehWidth.toInt(), bokehWidth.toInt()) {
            colorBuffer()
            depthBuffer()
        }

        val sourceRect = Rectangle(0.0, 0.0, bokehWidth, bokehWidth)

        val waveCanopy1Parameters = Vector4.uniform(0.0,1.0)
        val waveCanopy2Parameters = Vector4.uniform(0.0,1.0)

        // -- create blur filter
        val blurCanopy = BoxBlur()
        blurCanopy.window = 15
        blurCanopy.spread = 1.0
        blurCanopy.gain = 1.0

        val blurBlob = BoxBlur()
        blurBlob.window = 5
        blurBlob.spread = 1.0

        val blurFinal = BoxBlur()
        blurFinal.window = 2
        blurFinal.spread = 1.0

        extend(Screenshots())
        extend {
            drawer.isolatedWithTarget(upperCanopy) {
                ortho(upperCanopy)
                videoPlayer1.draw(drawer)
            }
            drawer.isolatedWithTarget(lowerCanopy) {
                ortho(lowerCanopy)
                videoPlayer2.draw(drawer)
            }

            val canopy1Corner = Vector2(-2*margin+margin*(waveCanopy1Parameters[0] +0.2*cos(seconds*waveCanopy1Parameters[2] + waveCanopy1Parameters[3])),
                -2*margin+margin*(waveCanopy1Parameters[1] + 0.2*sin(seconds*waveCanopy1Parameters[3] + waveCanopy1Parameters[2])))
            val canopy2Corner = Vector2(-2*margin+margin*(waveCanopy2Parameters[0] +0.2*cos(seconds*waveCanopy2Parameters[2] + waveCanopy2Parameters[3])),
                -2*margin+margin*(waveCanopy2Parameters[1] + 0.2*sin(seconds*waveCanopy2Parameters[3] + waveCanopy2Parameters[2])))

            drawer.isolatedWithTarget(preppedCanopy1) {
                ortho()
                clear(ColorRGBa.WHITE)
                image(upperCanopy.colorBuffer(0), canopy1Corner, upperCanopy.width.toDouble(), upperCanopy.height.toDouble())
            }
            val canopy1Buffer = preppedCanopy1.colorBuffer(0)
            canopy1Buffer.shadow.download()

            // blur to get smooth contours of medium holes
            blurBlob.apply(canopy1Buffer, blurred.colorBuffer(0))
            var blurredBuffer = blurred.colorBuffer(0)
            blurredBuffer.shadow.download()

            // function to find contours, from tutorial
            fun f(v: Vector2): Double {
                val iv = v.toInt()
//                val d = if (iv.x >= 0 && iv.y >= 0 && iv.x < fern.width && iv.y < fern.height) fern.shadow[iv.x, iv.y].luminance else 0.0
                val d =
                    if (iv.x >= 0 && iv.y >= 0 && iv.x < blurred.width && iv.y < blurred.height) blurredBuffer.shadow[iv.x, iv.y].luminance else 0.0
                return cos(d * PI)
            }

            // find contours, and centroid of blobs, and check if blob is bright
            val contours = findContours(::f, drawer.bounds.offsetEdges(32.0), 4.0)
            val centroids = mutableListOf<Vector2>()
            for (contour in contours) {
                if (contour.closed && (contour.bounds.area < bokehThreshold)) {
                    var centroid = Vector2(0.0, 0.0)
                    val length = contour.length

                    for (segment in contour.segments) {
                        centroid += segment.length * segment.position(0.5)
                    }
                    centroid /= length

                    if (blurredBuffer.shadow[centroid.x.toInt(), centroid.y.toInt()].luminance > 0.1) {
                        centroids += centroid
                    }
                }
            }

            // blur to canopy
            blurCanopy.apply(canopy1Buffer, blurred.colorBuffer(0))
            blurBlob.apply(lowerCanopy.colorBuffer(0), blurredCanopy2.colorBuffer(0))
            val canopy2Image = blurredCanopy2.colorBuffer(0)

            drawer.isolatedWithTarget(preppedCanopy2) {
                ortho()
                image(canopy2Image, canopy2Corner, canopy2Image.width.toDouble(), canopy2Image.height.toDouble())
            }
            drawer.isolatedWithTarget(shadows) {
                // mask directly
                ortho()
                clear(ColorRGBa.BLACK)
                drawStyle.blendMode = BlendMode.OVER
                image(blurredBuffer)
                drawStyle.blendMode = BlendMode.MULTIPLY
                image(preppedCanopy2.colorBuffer(0))

//                 mask centroids
                for (centroid in centroids) {
                    drawer.isolatedWithTarget(masked) {
                        ortho()
                        clear(ColorRGBa.BLACK)
                        drawStyle.blendMode = BlendMode.OVER
                        image(blurredBuffer, -centroid.x+bokehRadius, -centroid.y+bokehRadius)
                        drawStyle.blendMode = BlendMode.MULTIPLY
                        image(bokeh)
                        image(preppedCanopy2.colorBuffer(0), -centroid.x+bokehRadius, -centroid.y+bokehRadius)
                    }
                    drawStyle.blendMode = BlendMode.ADD
                    val x = centroid.x-bokehRadius + (width - centroid.x) * (1.0 - mag)/mag
                    val y = centroid.y-bokehRadius + (height - centroid.y) * (1.0 - mag)/mag
                    val bokehTargetRect = Rectangle(x, y, bokehWidth, bokehWidth)
                    image(masked.colorBuffer(0), sourceRect, bokehTargetRect)
                }
            }

            drawer.clear(ColorRGBa(grayLevel, grayLevel, grayLevel))
            drawer.drawStyle.blendMode = BlendMode.ADD
            blurFinal.apply(shadows.colorBuffer(0), blurredFinal.colorBuffer(0))
            drawer.translate(width.toDouble(),height.toDouble())
            drawer.rotate(180.0)
            drawer.image(blurredFinal.colorBuffer(0),0.0,0.0,width.toDouble(), height.toDouble())
        }
    }
}
