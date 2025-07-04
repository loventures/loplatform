/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package loi.cp.login

import com.learningobjects.cpxp.component.ComponentImplementation
import com.learningobjects.cpxp.component.annotation.{Component, Configuration}
import loi.cp.integration.AbstractSystem

@Component(name = "Login Redirect")
class RedirectLoginSystem extends AbstractSystem[RedirectLoginSystemComponent] with RedirectLoginSystemComponent:
  override def update(system: RedirectLoginSystemComponent): RedirectLoginSystemComponent =
    _self.setUrl(system.getRedirectUrl)
    super.update(system)

  @Configuration(label = "Redirect URL", size = 128, order = 10)
  override def getRedirectUrl: String = _self.getUrl

  def setRedirectUrl(redirectUrl: String): Unit = _self.setUrl(redirectUrl)

  override def getLoginComponent: RedirectLoginComponent =
    new RedirectLoginComponent with ComponentImplementation:
      val componentInstance       = RedirectLoginSystem.this.getComponentInstance
      override def getId          = RedirectLoginSystem.this.getId
      override def getName        = RedirectLoginSystem.this.getName
      override def getRedirectUrl = RedirectLoginSystem.this.getRedirectUrl

  override def logout(): String = "/" // TODO: support a logout redirect url
end RedirectLoginSystem
