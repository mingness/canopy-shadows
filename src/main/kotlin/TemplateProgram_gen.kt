import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter

fun main() = application {
    configure {
//        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        width = 400
        height = 300
    }

    program {
        val leaf = shadeStyle {
            fragmentPreamble = """
          const float PI = 3.1415926538;
          const float PI_2 = PI * 2.;
          const float PI_HALF = PI / 2.;
          
          float leafSdf(in vec2 coord) {
              float angle = atan(coord.x, coord.y) + PI_HALF + p_seconds;
              float dist = length(coord);
              return
                  dist
                  + (sin(angle) + 1.) * .5
                  + (cos(angle * p_segments) + 1.) * p_segmentFactor;    
          }
        """.trimIndent()
            fragmentTransform = """
          vec2 st = (c_boundsPosition.xy - vec2(.5)) * 2.0;
          float sdf = leafSdf(st);
          float luma = smoothstep(p_smoothing, 1.0, sdf);
//          x_fill.rgb = vec3(luma);
          x_fill.a = 1.0-luma;
        """.trimIndent()
        }
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.stroke = null
            drawer.fill = ColorRGBa.RED
            leaf.parameter("seconds", seconds)
            leaf.parameter("segments", 10)
            leaf.parameter("segmentFactor", 0.1)
            leaf.parameter("smoothing", 0.8)
            drawer.shadeStyle = leaf
            drawer.rectangle(0.0, 0.0, 50.0, 100.0)
            drawer.rectangle(100.0, 0.0, 100.0, 100.0)
            drawer.circle(100.0, 100.0, 100.0)
        }
    }
}
