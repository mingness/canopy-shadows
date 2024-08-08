import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
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
        val videoPlayer1 = VideoPlayerFFMPEG.fromDevice("/dev/video2")
        val videoPlayer2 = VideoPlayerFFMPEG.fromDevice("/dev/video4")
        videoPlayer1.play()
        videoPlayer2.play()
        extend {
            drawer.clear(ColorRGBa.BLACK)
            videoPlayer1.draw(drawer, 0.0,0.0)
            videoPlayer2.draw(drawer, 640.0,0.0)
        }

    }
}
