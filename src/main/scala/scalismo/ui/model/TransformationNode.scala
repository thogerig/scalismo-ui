package scalismo.ui.model

import scalismo.geometry._3D
import scalismo.registration.RigidTransformation
import scalismo.ui.event.Event
import scalismo.ui.model.capabilities.{Grouped, Removeable}

import scala.util.{Failure, Success, Try}

object GenericTransformationsNode {

  object event {

    case class TransformationsChanged(source: TransformationCollectionNode) extends Event

  }

}


object ShapeModelTransformationsNode {

  object event {

    case class ShapeModelTransformationsChanged(source: ShapeModelTransformationsNode) extends Event

  }

}

trait TransformationCollectionNode extends SceneNodeCollection[TransformationNode[_]] {

  val parent: GroupNode

  override protected def add(child: TransformationNode[_]): Unit = {
    listenTo(child)
    super.addToFront(child)
  }

  override def remove(child: TransformationNode[_]): Unit = {
    deafTo(child)
    super.remove(child)
  }
}


class GenericTransformationsNode(override val parent: GroupNode) extends TransformationCollectionNode {
  override val name: String = "Generic transformations"

  def add[T <: PointTransformation](transformation: T, name: String): TransformationNode[T] = {
    val node = TransformationNode(this, transformation, name)
    add(node)
    node
  }

  def combinedTransformation: PointTransformation = {
    val transforms = children.map(_.transformation.asInstanceOf[PointTransformation])
    transforms.foldLeft(PointTransformation.Identity: PointTransformation) { case (first, second) => first compose second }
  }

  override protected def add(child: TransformationNode[_]): Unit = {
    super.add(child)
    publishEvent(GenericTransformationsNode.event.TransformationsChanged(this))
  }

  override def remove(child: TransformationNode[_]): Unit = {
    super.remove(child)
    publishEvent(GenericTransformationsNode.event.TransformationsChanged(this))
  }

  reactions += {
    case TransformationNode.event.TransformationChanged(_) =>
      publishEvent(GenericTransformationsNode.event.TransformationsChanged(this))
  }
}



class ShapeModelTransformationsNode(override val parent: GroupNode) extends TransformationCollectionNode {
  override val name: String = "Shape model transformations"

  private [ui] var _poseTransform : Option [ShapeModelTransformationComponentNode[RigidTransformation[_3D]]] = None
  private [ui] var _shapeTransform : Option [ShapeModelTransformationComponentNode[DiscreteLowRankGpPointTransformation]] = None


  def addPoseTransformation(transformation: RigidTransformation[_3D], name: String): Try[ShapeModelTransformationComponentNode[RigidTransformation[_3D]]] = {

    if(_poseTransform.isDefined) {
      Failure(new Exception("The group already contains a rigid transformation as part of the Shape Model Transformation. Remove existing first"))
    }else {
      val node = ShapeModelTransformationComponentNode(this, transformation, name)
      _poseTransform = Some(node)
      add(node)
      Success(node)
    }
  }

  def addGaussianProcessTransformation(transformation: DiscreteLowRankGpPointTransformation, name: String): Try[ShapeModelTransformationComponentNode[DiscreteLowRankGpPointTransformation]] = {

    if(_shapeTransform.isDefined) {
      Failure(new Exception("The group already contains a GP transformation as part of the Shape Model Transformation. Remove existing first"))
    }else {
      val node = ShapeModelTransformationComponentNode(this, transformation, name)
      _shapeTransform = Some(node)
      add(node)
      Success(node)
    }
  }

  def removePoseTransformation(): Unit = {
    _poseTransform.map(remove)
    _poseTransform = None
  }

  def removeGaussianProcessTransformation(): Unit = {
    _shapeTransform.map(remove)
    _shapeTransform = None
  }

  protected def add(child: ShapeModelTransformationComponentNode[_]): Unit = {
    listenTo(child)
    super.addToFront(child)
    publishEvent(ShapeModelTransformationsNode.event.ShapeModelTransformationsChanged(this))
  }

  protected def remove(child: ShapeModelTransformationComponentNode[_]): Unit = {
    deafTo(child)
    super.remove(child)
    publishEvent(ShapeModelTransformationsNode.event.ShapeModelTransformationsChanged(this))
  }

    def combinedTransformation: Option[PointTransformation] = {
      val res = for{
        shapeTrans <- _shapeTransform
        poseTrans <- _poseTransform
      } yield {
        Some(poseTrans.transformation compose shapeTrans.transformation)
      }
      res.getOrElse(_poseTransform.map(_.transformation))
    }

  reactions += {
    case TransformationNode.event.TransformationChanged(_) =>
      publishEvent(ShapeModelTransformationsNode.event.ShapeModelTransformationsChanged(this))
  }
}



class ShapeModelTransformationComponentNode[T <: PointTransformation] private(override val parent: ShapeModelTransformationsNode, initialTransformation: T, override val name: String)
  extends TransformationNode[T](parent, initialTransformation, name)


object ShapeModelTransformationComponentNode {
  def apply(parent: ShapeModelTransformationsNode, initialTransformation: RigidTransformation[_3D], name: String) = new ShapeModelTransformationComponentNode(parent, initialTransformation, name)

  def apply(parent: ShapeModelTransformationsNode, initialTransformation: DiscreteLowRankGpPointTransformation, name: String) = new ShapeModelTransformationComponentNode(parent, initialTransformation, name)
}



object TransformationNode {
  def apply[T <: PointTransformation](parent: TransformationCollectionNode, transformation: T, name: String): TransformationNode[T] = {
    new TransformationNode(parent, transformation, name)
  }

  object event {

    case class TransformationChanged[T <: PointTransformation](source: TransformationNode[T]) extends Event

  }

}

class TransformationNode[T <: PointTransformation](override val parent: TransformationCollectionNode, initialTransformation: T, override val name: String) extends SceneNode with Grouped with Removeable {
  private var _transformation: T = initialTransformation

  def transformation: T = _transformation

  def transformation_=(newTransformation: T): Unit = {
    _transformation = newTransformation
    publishEvent(TransformationNode.event.TransformationChanged(this))
  }

  override def remove(): Unit = parent.remove(this)

  override def group: GroupNode = parent.parent
}

