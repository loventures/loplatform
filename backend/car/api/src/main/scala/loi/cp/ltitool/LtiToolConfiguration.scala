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

package loi.cp.ltitool

/** A representation of the configurations available for a globally configured Lti Tool.
  * @param defaultConfiguration
  *   the default configuration to apply to Lti content items which point to this Lti Tool.
  * @param instructorEditable
  *   A collection of values determining whether certain configuration values in [[defaultConfiguration]] are modifiable
  *   in the placement context.
  */
case class LtiToolConfiguration(
  defaultConfiguration: LtiLaunchConfiguration,
  instructorEditable: EditableLtiConfiguration
):

  def customParamIsUneditable(param: String): Boolean =
    uneditableCustomParameters.contains(param)

  def uneditableCustomParameters: Seq[String] =
    defaultConfiguration.customParameters.keys
      .filterNot(instructorEditable.editableCustomParameters.toSet)
      .toSeq

  /** Creates a 'filtered' version of this LtiToolConfiguration. Returns the same LtiToolConfiguration, without the tool
    * secret if it is not editable.
    * @return
    */
  def filtered: LtiToolConfiguration =
    this.copy(
      defaultConfiguration.copy(
        secret =
          if instructorEditable.secret then defaultConfiguration.secret
          else None
      ),
      instructorEditable
    )
end LtiToolConfiguration
