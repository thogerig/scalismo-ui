package scalismo.ui.view.perspective

import scalismo.ui.view.{ ViewportPanel, ScalismoFrame, ViewportPanel3D }

import scala.swing.BorderPanel

class Single3DViewportPerspective(override val frame: ScalismoFrame, override val factory: PerspectiveFactory) extends BorderPanel with Perspective {
  val viewport = new ViewportPanel3D(frame)

  override val viewports: List[ViewportPanel] = List(viewport)

  layout(viewport) = BorderPanel.Position.Center
}

object Single3DViewportPerspective extends PerspectiveFactory {
  override def instantiate(frame: ScalismoFrame): Perspective = new Single3DViewportPerspective(frame, this)

  override val perspectiveName: String = "Single 3D viewport"
}