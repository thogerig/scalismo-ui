/*
 * Copyright (C) 2016  University of Basel, Graphics and Vision Research Group 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package scalismo.ui.api

import scalismo.ui.view.ScalismoFrame

/**
 * This typeclass needs to be implemented if callbacks should be allowed for a view V
 */
protected[api] trait HandleCallback[V] {
  // calls function f if a node with type A has been added to the group g
  def registerOnAdd[R](g: Group, f: V => R, frame: ScalismoFrame)

  // calls function f if a node with type A has been removed from the group g
  def registerOnRemove[R](g: Group, f: V => R, frame: ScalismoFrame)

}

object HandleCallback {
  def apply[A](implicit a: HandleCallback[A]): HandleCallback[A] = a

}
