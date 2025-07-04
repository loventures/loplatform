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

package loi.cp.startup

import com.learningobjects.cpxp.component.{ComponentArchive, ComponentEnvironment}
import scaloi.syntax.`class`.*
import com.learningobjects.cpxp.startup.{StartupTask, StartupTaskBinding, StartupTaskInfo, StartupTaskScope}
import loi.cp.startup.DependencyOrder.SeqDependencyOrderOps

import scala.jdk.CollectionConverters.*
import scala.compat.java8.OptionConverters.*
import scalaz.syntax.std.boolean.*

/** Utilities for working with startup tasks.
  */
object StartupTasks:

  /** Startup task class. */
  type StartupClass = Class[? <: StartupTask]

  /** Archive and its startup tasks.
    * @param archive
    *   the component archive
    * @param tasks
    *   the archive tasks
    */
  case class ArchiveTasks(archive: ComponentArchive, tasks: Seq[StartupTaskInfo.Any])

  /** Enhancements on component environments for startup tasks.
    * @param env
    *   the component environment
    */
  implicit class ComponentEnvironmentOps(val env: ComponentEnvironment):

    /** Returns the system startup tasks for this component environment in dependency order.
      *
      * @return
      *   the system startup tasks
      */
    def systemStartupTasks: Seq[StartupTaskInfo.Any] =
      startupTasks(StartupTaskScope.System)

    /** Returns the system startup tasks for this component environment in dependency order.
      *
      * @param overlord
      *   whether the domain is the overlord domain
      * @return
      *   the system startup tasks
      */
    def domainStartupTasks(overlord: Boolean): Seq[StartupTaskInfo.Any] =
      startupTasks(overlord.fold(StartupTaskScope.Overlord, StartupTaskScope.Domain))

    /** Returns the startup tasks for this component environment in dependency order.
      *
      * @param scope
      *   the startup task scope to look for
      * @return
      *   the startup tasks
      */
    def startupTasks(scope: StartupTaskScope): Seq[StartupTaskInfo.Any] =
      archiveOrder(environmentTasks(scope)).flatMap(taskOrder)

    /** Return the dependency ordering of a sequence of archives.
      *
      * @param archives
      *   the archives
      * @return
      *   the dependency ordering of the archives
      */
    def archiveOrder(archives: Seq[ArchiveTasks]): Seq[ArchiveTasks] =
      archives.dependencyOrder valueOr { loop =>
        throw new RuntimeException(s"Unsatisfiable archive dependency: $loop")
      }

    /** Return the dependency ordering of an archive's tasks.
      *
      * @param archive
      *   the archive and its tasks
      * @return
      *   the dependency ordering of the tasks
      */
    def taskOrder(archive: ArchiveTasks): Seq[StartupTaskInfo.Any] =
      archive.tasks.dependencyOrder valueOr { loop =>
        // If you find yourself here, you have a startup task that has expressed
        // a dependency on another startup task that is either in a different
        // module (CAR) or has a different type (per-domain vs system).
        throw new RuntimeException(s"Unsatisfiable startup task dependency: $loop")
      }

    /** Returns a list of component archives and their startup tasks.
      *
      * @param scope
      *   the startup task scope to look for
      * @return
      *   the archives and their tasks
      */
    def environmentTasks(scope: StartupTaskScope): Seq[ArchiveTasks] =
      componentArchives(scope == StartupTaskScope.System)
        .map(a => ArchiveTasks(a, archiveTasks(_.taskScope.matches(scope))(a)))
        .filterNot(_.tasks.isEmpty)

    /** Returns the component archives for this component environment.
      *
      * @param system
      *   whether to look at all archives or per-domain archives
      * @return
      *   the component archives
      */
    def componentArchives(system: Boolean): Seq[ComponentArchive] =
      system.fold(env.getArchives, env.getAvailableArchives).asScala.toSeq

    /** Returns the startup tasks for a component archive.
      *
      * @param p
      *   a predicate that selects which tasks to include
      * @param archive
      *   the component archive
      * @return
      *   the startup tasks
      */
    private def archiveTasks(p: StartupTaskBinding => Boolean)(archive: ComponentArchive): Seq[StartupTaskInfo.Any] =
      archive.getBoundClasses.asScala.toSeq.flatMap(startupTaskInfo(p))

    /** Filters classes to return only startup classes matching a startup task predicate.
      * @param p
      *   a predicate that selects which tasks to include
      * @param cls
      *   the class under consideration
      * @return
      *   the class and its binding, if it is a startup task class that matches the predicate
      */
    private def startupTaskInfo(p: StartupTaskBinding => Boolean)(cls: Class[?]): Option[StartupTaskInfo.Any] =
      for
        startupClass <- startupClass(cls)
        binding      <- startupClass.annotation[StartupTaskBinding]
        if p(binding)
      yield StartupTaskInfo(startupClass, binding)

    /** Filters classes to return only startup classes.
      * @param cls
      *   the class under consideration
      * @return
      *   the class, if it is a startup task class
      */
    private def startupClass(cls: Class[?]): Option[StartupClass] =
      classOf[StartupTask]
        .isAssignableFrom(cls)
        .option(cls.asInstanceOf[StartupClass])
  end ComponentEnvironmentOps

  /** Dependency order evidence for component archives.
    */
  implicit val ComponentArchiveDependencyOrder: DependencyOrder[ArchiveTasks] =
    new DependencyOrder[ArchiveTasks]:

      /** Secondary sort is alphabetic on the component archive identifier. */
      override def sorted(as: Seq[ArchiveTasks]): Seq[ArchiveTasks] =
        as.sortBy(_.archive.getIdentifier)

      /** Dependencies are based on the component archive module dependencies. An archive depends on its build
        * dependencies and their implementations.
        */
      override def dependsOn(a: ArchiveTasks): ArchiveTasks => Boolean =
        val direct   = a.archive.getDependencies.asScala
        val implDeps = direct flatMap (_.getImplementation.asScala map (_.getDependencies.asScala) getOrElse Nil)
        a => direct.contains(a.archive) || implDeps.contains(a.archive)

  /** Dependency order evidence for startup tasks.
    */
  implicit val StartupTaskDependencyOrder: DependencyOrder[StartupTaskInfo.Any] =
    new DependencyOrder[StartupTaskInfo.Any]:

      /** Secondary sort is alphabetic on the class name. */
      override def sorted(as: Seq[StartupTaskInfo.Any]): Seq[StartupTaskInfo.Any] =
        as.sortBy(_.startupClass.getName)

      /** Dependencies are based on the startup task binding. */
      override def dependsOn(a: StartupTaskInfo.Any): StartupTaskInfo.Any => Boolean =
        b => a.binding.runAfter.contains(b.startupClass)
end StartupTasks
