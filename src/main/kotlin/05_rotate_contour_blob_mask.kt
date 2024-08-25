import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.drawImage
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.color.presets.DARK_GRAY
import org.openrndr.extra.color.presets.DIM_GRAY
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.math.Vector2
import org.openrndr.math.times
import org.openrndr.shape.Rectangle
import kotlin.math.*

fun main() = application {
    configure {
//        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
//        hideCursor = true
        width = 600
        height = 600
    }
    program {
        val canopy1 = loadImage("data/images/fern.jpg")
        val canopy2 = loadImage("data/images/duckheads.jpg")

        val bokehThreshold = 5000
        val whiteThreshold = 3000
        val bokehRadius = 40.0
        val bokehWidth = bokehRadius*2
        val mag = 5.0 / 3.0

        val imageWidth = canopy1.width
        val imageHeight = canopy1.height

        val rotated = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }
        val blurred = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }
        val blurredCanopy2 = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }
        val bokeh = drawImage(bokehWidth.toInt(), bokehWidth.toInt()) {
            drawer.clear(ColorRGBa.BLACK)
            drawer.fill = ColorRGBa.WHITE
            drawer.circle(bokehRadius,bokehRadius,bokehRadius)
        }
        val masked = renderTarget(bokehWidth.toInt(), bokehWidth.toInt()) {
            colorBuffer()
            depthBuffer()
        }
        val shadows = renderTarget(imageWidth, imageHeight) {
            colorBuffer()
            depthBuffer()
        }

        val sourceRect = Rectangle(0.0, 0.0, bokehWidth, bokehWidth)


        // -- create blur filter
        val blurCanopy = BoxBlur()
        blurCanopy.window = 15
        blurCanopy.spread = 1.0
        blurCanopy.gain = 0.5

        val blurBlob = BoxBlur()
        blurBlob.window = 5
        blurBlob.spread = 1.0

        extend(Screenshots())
        extend {
            // simulate video
            drawer.isolatedWithTarget(rotated) {
                ortho(rotated)
                clear(ColorRGBa.WHITE)
                translate(width / 2.0, height / 2.0)
                rotate(seconds * 360 / 30)
                image(canopy1, -width / 2.0, -height / 2.0)
            }
            val animation = rotated.colorBuffer(0)
            animation.shadow.download()

            // blur to get smooth contours of medium holes
            blurBlob.apply(animation, blurred.colorBuffer(0))
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
//            println(contours)
            val centroids = mutableListOf<Vector2>()
//            val areas = mutableListOf<Double>()
            for (contour in contours) {
                if (contour.closed && (contour.bounds.area < bokehThreshold)) {
                    var centroid = Vector2(0.0, 0.0)
                    val length = contour.length

                    for (segment in contour.segments) {
                        centroid += segment.length * segment.position(0.5)
                    }
                    centroid /= length

//                    println(blurredBuffer.shadow[centroid.x.toInt(), centroid.y.toInt()].luminance)
                    if (blurredBuffer.shadow[centroid.x.toInt(), centroid.y.toInt()].luminance > 0.1) {
                        centroids += centroid
//                        areas += contour.bounds.area
                    }
                }
            }
//            println(areas)
//            println("number of centroids: ${centroids.size}")

            // blur to canopy
            blurCanopy.apply(animation, blurred.colorBuffer(0))
            blurBlob.apply(canopy2, blurredCanopy2.colorBuffer(0))


            drawer.isolatedWithTarget(shadows) {
                // mask directly
                ortho()
                clear(ColorRGBa.BLACK)
                drawStyle.blendMode = BlendMode.OVER
                image(blurredBuffer, 0.0, 0.0, blurredBuffer.width.toDouble(), blurredBuffer.height.toDouble())
                drawStyle.blendMode = BlendMode.MULTIPLY
                image(blurredCanopy2.colorBuffer(0), 0.0, 0.0, shadows.width.toDouble(), shadows.height.toDouble())

//                 mask centroids
                for (centroid in centroids) {
                    drawer.isolatedWithTarget(masked) {
                        ortho()
                        clear(ColorRGBa.BLACK)
                        drawStyle.blendMode = BlendMode.OVER
                        image(blurredBuffer, -centroid.x+bokehRadius, -centroid.y+bokehRadius)
                        drawStyle.blendMode = BlendMode.MULTIPLY
                        image(bokeh)
                        image(blurredCanopy2.colorBuffer(0), -centroid.x+bokehRadius, -centroid.y+bokehRadius)
                    }
                    drawStyle.blendMode = BlendMode.ADD
                    val x = centroid.x-bokehRadius + (width - centroid.x) * (1.0 - mag)/mag
                    val y = centroid.y-bokehRadius + (height - centroid.y) * (1.0 - mag)/mag
//                    println("centroid_x = ${centroid.x}, centroid_y = ${centroid.y}, x = $x, y = $y")
                    val targetRect = Rectangle(x, y, bokehWidth, bokehWidth)
                    image(masked.colorBuffer(0), sourceRect, targetRect)
                }
            }

            drawer.clear(ColorRGBa.BLACK)
            drawer.drawStyle.blendMode = BlendMode.OVER
            drawer.image(shadows.colorBuffer(0))
        }
    }
}


