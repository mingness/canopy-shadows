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
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.math.Vector4
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import kotlin.math.*

fun main() = application {
    configure {
//        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
//        hideCursor = true
        height = 480
        width = 1280
    }
    program {
        println(VideoPlayerFFMPEG.listDeviceNames())
        val videoPlayer1 = VideoPlayerFFMPEG.fromDevice("/dev/video0")
        val videoPlayer2 = VideoPlayerFFMPEG.fromDevice("/dev/video2")
        videoPlayer1.play()
        videoPlayer2.play()
        extend {
            drawer.clear(ColorRGBa.BLACK)
            videoPlayer1.draw(drawer, 0.0,0.0)
            videoPlayer2.draw(drawer, 640.0,0.0)
        }

    }
}
