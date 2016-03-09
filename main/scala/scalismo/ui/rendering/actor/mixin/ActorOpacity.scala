package scalismo.ui.rendering.actor.mixin

import scalismo.ui.model.properties._
import scalismo.ui.rendering.actor.{ DynamicActor, SingleActor }

trait ActorOpacity extends SingleActor with DynamicActor {
  def opacity: OpacityProperty

  listenTo(opacity)

  reactions += {
    case NodeProperty.event.PropertyChanged(p) if p eq opacity => setAppearance()
  }

  private def setAppearance() = {
    GetProperty().SetOpacity(opacity.value)
    requestRendering()
  }

  setAppearance()

}
