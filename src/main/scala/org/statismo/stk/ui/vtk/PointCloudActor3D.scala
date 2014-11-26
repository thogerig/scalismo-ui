package org.statismo.stk.ui.vtk

import org.statismo.stk.core.geometry.Point3D
import org.statismo.stk.ui.PointCloud.PointCloudRenderable3D
import org.statismo.stk.ui.visualization.VisualizationProperty
import vtk.{vtkGlyph3D, vtkPoints, vtkPolyData, vtkSphereSource}

class PointCloudActor3D(renderable: PointCloudRenderable3D) extends PolyDataActor with ColorableActor with ClickableActor {
  private lazy val sphere = new vtkSphereSource
  override lazy val color = renderable.color
  override lazy val opacity = renderable.opacity
  lazy val radius = renderable.radius
  listenTo(radius)

  val points = new vtkPoints {
    renderable.source.peer.foreach { point =>
      InsertNextPoint(point(0), point(1), point(2))
    }
  }

  val polydata = new vtkPolyData {
    SetPoints(points)
  }

  val glyph = new vtkGlyph3D {
    SetSourceConnection(sphere.GetOutputPort)
    SetInputData(polydata)
  }

  mapper.SetInputConnection(glyph.GetOutputPort)

  this.GetProperty().SetInterpolationToGouraud()
  setGeometry()

  reactions += {
    case VisualizationProperty.ValueChanged(s) => if (s eq radius) setGeometry()
  }

  def setGeometry() = this.synchronized {
    sphere.SetRadius(renderable.radius.value)
    sphere.Modified()
    glyph.Update()
    glyph.Modified()
    mapper.Modified()
    publishEdt(VtkContext.RenderRequest(this))
  }

  override def onDestroy() = this.synchronized {
    deafTo(radius)
    super.onDestroy()
    glyph.Delete()
    polydata.Delete()
    points.Delete()
    sphere.Delete()
  }

  override def clicked(point: Point3D): Unit = {
    // the click is on the surface of a sphere, but actually "means" the center of the sphere.
    val cloudPoints = renderable.source.peer
    // just in case
    if (cloudPoints.size > 0) {
      val vector = point.toVector
      val vectorsWithIndex = cloudPoints.map(_.toVector - vector).zipWithIndex
      val minIndex = vectorsWithIndex.minBy(_._1.norm2)._2
      renderable.source.addLandmarkAt(cloudPoints(minIndex).asInstanceOf[Point3D])
    }
  }
}