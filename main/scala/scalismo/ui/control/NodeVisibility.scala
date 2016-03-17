package scalismo.ui.control

import scalismo.ui.control.NodeVisibility.State._
import scalismo.ui.control.NodeVisibility.{ Context, Node, State }
import scalismo.ui.event.{ Event, ScalismoPublisher }
import scalismo.ui.model.SceneNode
import scalismo.ui.model.capabilities.RenderableSceneNode
import scalismo.ui.view._
import scalismo.ui.view.perspective.Perspective

import scala.collection.mutable

object NodeVisibility {

  // convenience type aliases
  type Node = RenderableSceneNode
  type Context = ViewportPanel

  sealed trait State {

  }

  object State {

    case object Visible extends State

    case object Invisible extends State

    case object PartlyVisible extends State

  }

  //  import scala.language.implicitConversions
  //
  //  object Visible {
  //    implicit def visibleAsBoolean(v: Visible): Boolean = v.visible
  //  }
  //
  //  class Visible private[NodeVisibility] (map: NodeVisibility, node: SceneNode) {
  //    def visible: Boolean = map.isVisible(node)
  //
  //    def visible_=(show: Boolean): Unit = map.setVisible(node, map.allViewports, show)
  //
  //    def update(viewports: List[ViewportPanel], show: Boolean): Unit = {
  //      map.setVisible(node, viewports, show)
  //    }
  //
  //    def apply(viewports: List[ViewportPanel]): Boolean = {
  //      map.isVisible(node, viewports)
  //    }
  //
  //    def update(viewport: ViewportPanel, show: Boolean): Unit = {
  //      map.setVisible(node, viewport, show)
  //    }
  //
  //    def apply(viewportPanel: ViewportPanel): Boolean = {
  //      map.isVisible(node, viewportPanel)
  //    }
  //
  //    override def toString: String = {
  //      s"Visible [node=$node, hidden in ${map.toString(node)}]"
  //    }
  //  }
  //
  //  class SceneNodeWithVisibility(node: SceneNode)(implicit frame: ScalismoFrame) {
  //    private val map: NodeVisibility = frame.sceneControl.nodeVisibility
  //
  //    private val _visibility: Visible = new Visible(map, node)
  //
  //    def visible: Visible = _visibility
  //
  //    def visible_=(nv: Boolean): Unit = _visibility.visible = nv
  //
  //  }

  object event {

    case class NodeVisibilityChanged(node: SceneNode, viewport: ViewportPanel) extends Event

  }

  class RenderableNodeWithVisibility(node: RenderableSceneNode)(implicit frame: ScalismoFrame) {

  }

}

class NodeVisibility(frame: ScalismoFrame) extends ScalismoPublisher {
  // OLD IMPLEMENTATION
  //  def isVisible(node: SceneNode, viewports: List[ViewportPanel] = allViewports): Boolean = {
  //    viewports.forall(v => isVisible(node, v))
  //  }
  //
  //  def toString(node: SceneNode) = {
  //    hidden.get(node).toString
  //  }
  //
  //  def isVisible(node: SceneNode, viewport: ViewportPanel): Boolean = {
  //    !hidden.get(node).exists(_.contains(viewport))
  //  }
  //
  //  def setNodeVisibility(node: SceneNode, viewports: List[ViewportPanel], show: Boolean): Unit = {
  //    def nodeAndChildren(node: SceneNode): List[SceneNode] = {
  //      node :: node.children.flatMap(child => nodeAndChildren(child))
  //    }
  //
  //    nodeAndChildren(node).foreach { node =>
  //      setVisible(node, viewports, show)
  //    }
  //  }
  //
  //  private[NodeVisibility] def setVisible(node: SceneNode, viewport: ViewportPanel, show: Boolean): Unit = {
  //    setVisible(node, List(viewport), show)
  //  }
  //
  //  private[NodeVisibility] def setVisible(node: SceneNode, viewports: List[ViewportPanel], show: Boolean): Unit = {
  //    val previous = hidden.getOrElse(node, Set.empty)
  //    val (added, removed) = if (show) (Set.empty, viewports.distinct) else (viewports.distinct, Set.empty)
  //    val current = (previous -- removed) ++ added
  //    if (current != previous) {
  //      if (current.isEmpty) {
  //        hidden.remove(node)
  //      } else {
  //        hidden(node) = current
  //      }
  //      (removed ++ added).foreach { viewport =>
  //        publishEvent(NodeVisibility.event.NodeVisibilityChanged(node, viewport))
  //      }
  //    }
  //  }
  //
  //  private[NodeVisibility] def allViewports: List[ViewportPanel] = frame.perspective.viewports
  //
  //  private def handlePerspectiveChange(current: Perspective, previous: Perspective) = {
  //    val oldViewports = previous.viewports
  //    val newViewports = current.viewports
  //
  //    val newHidden: List[(SceneNode, List[ViewportPanel])] = {
  //      val old3DCount = oldViewports.collect { case _3d: ViewportPanel3D => _3d }.length
  //      val new3DViews = newViewports.collect { case _3d: ViewportPanel3D => _3d }
  //
  //      hidden.keys.toList.map { node =>
  //        if (!isVisible(node, oldViewports)) {
  //          // easy case: node was hidden in all viewports, so it just remains hidden
  //          (node, newViewports)
  //        } else {
  //          // we have to do some guesswork now.
  //
  //          // We'll assume that if a node was hidden in a particular 2D axis view, it should remain hidden for that axis
  //          val axesToHide = oldViewports.collect { case _2d: ViewportPanel2D if !isVisible(node, _2d) => _2d.axis }.distinct
  //          val hide2D = newViewports.collect { case _2d: ViewportPanel2D if axesToHide.contains(_2d.axis) => _2d }
  //
  //          // For 3D views, we'll assume that if a node was hidden in *strictly more* than half of the views, it should
  //          // remain hidden in *all* of the new 3D views. Otherwise, it gets shown.
  //          val hidden3D = oldViewports.collect { case _3d: ViewportPanel3D if !isVisible(node, _3d) => _3d }.length
  //          val hide3D = if (hidden3D > old3DCount / 2) new3DViews else Nil
  //
  //          // return the viewports to hide the object in
  //          (node, hide2D ++ hide3D)
  //        }
  //      }
  //    }
  //
  //    hidden.clear()
  //    newHidden.foreach {
  //      case (node, viewports) =>
  //        if (viewports.nonEmpty) {
  //          hidden(node) = viewports.toSet
  //          viewports.foreach { vp =>
  //            publishEvent(NodeVisibility.event.NodeVisibilityChanged(node, vp))
  //          }
  //        }
  //    }
  //  }
  //
  private val hiddenMap = new mutable.WeakHashMap[Node, Set[Context]]

  // lowest level: single node, non-empty set of contexts
  private def getStateInMap(node: Node, contexts: Set[Context]): State = {
    hiddenMap.get(node) match {
      case None => Visible
      case Some(hiddens) =>
        val intersection = hiddens.intersect(contexts)
        if (intersection.isEmpty) {
          Visible
        } else if (intersection == contexts) {
          Invisible
        } else {
          PartlyVisible
        }
    }
  }

  // slightly higher level: set of nodes, set of contexts
  private def getStateInMap(nodes: Set[Node], contexts: Set[Context]): State = {
    require(nodes.nonEmpty && contexts.nonEmpty)
    val nodeStates = nodes.map(node => getStateInMap(node, contexts))

    // Now we have a set of states. If the set contains a single value, then that's
    // our result. Otherwise, it is necessarily a mixed/partial visibility.
    if (nodeStates.size == 1) {
      nodeStates.toList.head
    } else {
      PartlyVisible
    }
  }

  // public API
  def getVisibilityState(nodes: Iterable[Node], viewports: Iterable[Context]): State = {
    val nodesSet = nodes.toSet
    val viewportsSet = viewports.toSet
    // there's no correct answer to "is nothing visible in nothing?" or similar
    require(nodesSet.nonEmpty && viewportsSet.nonEmpty)

    getStateInMap(nodesSet, viewportsSet)
  }

  // convenience public API
  def getVisibilityState(node: Node, viewports: Iterable[Context]): State = {
    getVisibilityState(List(node), viewports)

  }

  def getVisibilityState(nodes: Iterable[Node], viewport: Context): State = {
    getVisibilityState(nodes, List(viewport))
  }

  def getVisibilityState(node: Node, viewport: Context): State = {
    getVisibilityState(List(node), List(viewport))
  }

  def isVisible(node: RenderableSceneNode, viewport: ViewportPanel): Boolean = {
    getVisibilityState(node, viewport) == Visible
  }

  // lowest level: single node, non-empty set of contexts
  private def setStateInMap(node: Node, contexts: Set[Context], hide: Boolean): Unit = {
    hiddenMap.get(node) match {
      case None =>
        // No previous state; if we want to hide a node, we just add the contexts.
        // If we want the node to be shown, there's nothing to do anyway, because
        // absence of the node in the map signifies that it's visible.
        if (hide) {
          hiddenMap(node) = contexts
        }
      case Some(hiddens) =>
        if (hide) {
          hiddenMap(node) = hiddens union contexts
        } else {
          val resulting = hiddens diff contexts
          if (resulting.nonEmpty) {
            hiddenMap(node) = resulting
          } else {
            hiddenMap.remove(node)
          }
        }
    }
    // We publish the event unconditionally, even if nothing has actually changed.
    // This could be optimized in the future, if needed.
    contexts.foreach { viewport =>
      publishEvent(NodeVisibility.event.NodeVisibilityChanged(node, viewport))
    }
  }

  private def setStateInMap(nodes: Set[Node], contexts: Set[Context], hide: Boolean): Unit = {
    nodes.foreach { node =>
      setStateInMap(node, contexts, hide)
    }
  }

  // public API
  def setVisibility(nodes: Iterable[Node], viewports: Iterable[Context], show: Boolean): Unit = {
    val nodesSet = nodes.toSet
    val viewportsSet = viewports.toSet

    if (nodesSet.nonEmpty && viewportsSet.nonEmpty) {
      setStateInMap(nodesSet, viewportsSet, !show)
    }
  }

  // convenience public API

  def setVisibility(node: Node, viewports: Iterable[Context], show: Boolean): Unit = {
    setVisibility(List(node), viewports, show)
  }

  def setVisibility(nodes: Iterable[Node], viewport: Context, show: Boolean): Unit = {
    setVisibility(nodes, List(viewport), show)
  }

  def setVisibility(node: Node, viewport: Context, show: Boolean): Unit = {
    setVisibility(List(node), List(viewport), show)
  }

  private def handlePerspectiveChange(current: Perspective, previous: Perspective) = {
    // FIXME
  }

  def initialize(): Unit = {
    listenTo(frame.perspective)
  }

  reactions += {
    case PerspectivePanel.event.PerspectiveChanged(_, current, previous) if previous.isDefined => handlePerspectiveChange(current, previous.get)
  }
}
