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

package loi.cp.group.tasks

import java.util.logging.{Level, Logger}

import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.folder.FolderFacade
import com.learningobjects.cpxp.service.group.GroupFolderFacade
import com.learningobjects.cpxp.startup.{StartupTask, StartupTaskBinding, StartupTaskScope}
import loi.cp.admin.FolderParentFacade

/** Bootstrap all group folders.
  */
@StartupTaskBinding(
  version = 20161021,
  taskScope = StartupTaskScope.Domain
)
class GroupFolderCreationTask(
  facadeService: FacadeService
) extends StartupTask:

  private val logger: Logger =
    Logger.getLogger(classOf[GroupFolderCreationTask].getName)

  //
  // All supported group folders go here...
  //
  val folders: Seq[GroupFolder] = Seq(
    GroupFolder("folder-courses", "/Courses", "els.evolve.group.CourseAdminPage"),
    GroupFolder("folder-libraries", "/Libraries", "els.evolve.group.LibraryAdminPage"),
    GroupFolder("folder-demoCourses", "/DemoCourses", "els.evolve.group.DemoCourseAdminPage"),
    GroupFolder(
      "folder-integrationTestCourses",
      "/IntegrationTestCourses",
      "loi.cp.course.integration.IntegrationTestCourseAdminPage"
    ),
  )

  override def run(): Unit = folders foreach getOrCreateGroupFolder

  /** Represents a group folder to create.
    *
    * @param id
    *   Folder identifier. E.g., "folder-courses"
    * @param url
    *   E.g., "/Courses"
    * @param componentId
    *   Fully-qualified class name of the admin page. E.g., "els.evolve.group.ProgramAdminPage"
    */
  case class GroupFolder(id: String, url: String, componentId: String)

  /** Create a group folder. This replaces AbstractGroupAdminPage.createFolder (a PostDeploy)
    */
  private def getOrCreateGroupFolder(groupFolder: GroupFolder): FolderFacade =
    var folderFacade: FolderFacade =
      facadeService.getFacade(groupFolder.id, classOf[GroupFolderFacade])
    if folderFacade == null then
      logger.log(Level.INFO, s"Group folder <${groupFolder.id}> not found, creating...")
      val parent: FolderParentFacade = facadeService
        .getFacade(Current.getDomain, classOf[FolderParentFacade])
      folderFacade = parent.addFolder()
      folderFacade.setType("group")
      folderFacade.bindUrl(groupFolder.url)
      folderFacade.setIdStr(groupFolder.id)
      var clas: Class[?]             = getClass
      folderFacade.setIdentifier(groupFolder.componentId)
      logger.log(Level.INFO, s"Group folder <${groupFolder.id}> created: ${folderFacade.getId}")
    else logger.log(Level.INFO, s"Group folder <${groupFolder.id}> already exists: ${folderFacade.getId}")
    end if
    folderFacade
    // createGroupFolder
  end getOrCreateGroupFolder
end GroupFolderCreationTask
