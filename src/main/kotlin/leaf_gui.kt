import org.openrndr.application
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter

fun main() = application {
    program {
        val gui = GUI()

        val settings = object {
            @IntParameter("segments", 1, 40, order =  0)
            var segments: Int = 10

            @DoubleParameter("segment factor", 0.0, 1.0, order = 1)
            var segmentFactor: Double = 1.0

            @DoubleParameter("smoothing", 0.0, 1.0, order = 2)
            var smoothing: Double = 1.0
        }

        gui.add(settings, "leaf settings")

        val style = shadeStyle {
            fragmentPreamble = """
          const float PI = 3.1415926538;
          const float PI_2 = PI * 2.;
          const float PI_HALF = PI / 2.;
          
          float leafSdf(in vec2 coord) {
              float angle = atan(coord.x, coord.y) - PI_HALF;
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
          x_fill.rgb = vec3(luma);
        """.trimIndent()
        }
        extend(gui)
        extend {
            style.parameter("seconds", seconds)
            style.parameter("segments", settings.segments)
            style.parameter("segmentFactor", settings.segmentFactor)
            style.parameter("smoothing", settings.smoothing)
            drawer.shadeStyle = style
            drawer.rectangle(100.0, 100.0, 400.0, 400.0)
        }
    }
}
