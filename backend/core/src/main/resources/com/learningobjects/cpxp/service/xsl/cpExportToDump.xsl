<?xml version="1.0" encoding="UTF-8"?>

<!--
  ~ LO Platform copyright (C) 2007–2025 LO Ventures LLC.
  ~
  ~ This program is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU Affero General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ This program is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU Affero General Public License for more details.
  ~
  ~ You should have received a copy of the GNU Affero General Public License
  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.
  -->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cp="http://learningobjects.com/schema/cp/1.0">

  <xsl:template match="cp:Wiki">
    <xsl:call-template name="Site" />
  </xsl:template>

  <xsl:template match="cp:Blog">
    <xsl:call-template name="Site" />
  </xsl:template>

  <xsl:template match="cp:Journal">
    <xsl:call-template name="Site" />
  </xsl:template>

  <xsl:template match="cp:Podcast">
    <xsl:call-template name="Site" />
  </xsl:template>

  <xsl:template name="Site">
    <xsl:variable name="type" select="local-name()" />
    <Dump xmlns="http://learningobjects.com/schema/ug/1.0/dump"
            xmlns:xi="http://www.w3.org/2001/XInclude">
      <Current User="current-user" />
      <Item Id="{generate-id()}">
        <xsl:choose>
          <xsl:when test="$type = 'Journal'">
            <xsl:attribute name="Type">Blog</xsl:attribute>
          </xsl:when>
          <xsl:otherwise>
            <xsl:attribute name="Type">
              <xsl:value-of select="$type" />
            </xsl:attribute>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="@GradebookCreated = 'true'">
          <Data Type="gradebookCreated" Boolean="true" />
        </xsl:if>
        <xsl:choose>
          <xsl:when test="$type = 'Podcast'">
            <Data Type="disabled" Boolean="false" />
            <Data Type="iconName" String="Podcast" />
            <Data Type="archetype" Item="@template-basicPodcast" />
            <Data Type="createTime" Time="current" />
            <Data Type="prototype" Item="@template-basicPodcast" />
            <xsl:for-each select="cp:Episode">
              <Item Type="Episode" Id="episode-{generate-id()}">
                <xsl:attribute name="URL">/<xsl:value-of select="@Name" /></xsl:attribute>
                <Data Type="iconName" String="Podcast" />
                <Data Type="uiContext" Item="{generate-id(..)}" />
                <Data Type="body">
                  <xsl:value-of select="cp:Body" />
                </Data>
                <xsl:choose>
                  <xsl:when test="@MediaFilename">
                    <Data Type="Episode.mediaAttachment" Item="attachment-{generate-id()}" />
                    <Item Id="attachment-{generate-id()}" Type="Attachment">
                      <xsl:attribute name="URL"><xsl:value-of select="@MediaFilename" /></xsl:attribute>
                      <xsl:attribute name="Attachment">attach/<xsl:value-of select="@MediaFilename" /></xsl:attribute>
                      <Data Type="Attachment.disposition" String="inline" />
                      <Data Type="Attachment.fileName">
                        <xsl:attribute name="String"><xsl:value-of select="@MediaFilename" /></xsl:attribute>
                      </Data>
                      <Data Type="inheritedAccessControl" Item="episode-{generate-id()}" />
                    </Item>
                  </xsl:when>
                  <xsl:otherwise>
                    <Data Type="Episode.mediaUrl">
                      <xsl:attribute name="String"><xsl:value-of select="@MediaUrl" /></xsl:attribute>
                    </Data>
                  </xsl:otherwise>
                </xsl:choose>
                <xsl:if test="@MediaType">
                  <Data Type="Episode.mediaType">
                    <xsl:attribute name="String"><xsl:value-of select="@MediaType" /></xsl:attribute>
                  </Data>
                </xsl:if>
                <xsl:if test="@LocalId">
                  <Data Type="localId">
                    <xsl:attribute name="String"><xsl:value-of select="@LocalId" /></xsl:attribute>
                  </Data>
                </xsl:if>
                <Data Type="Import.id">
                  <xsl:attribute name="String">
                    <xsl:value-of select="@Id" />
                  </xsl:attribute>
                </Data>
                <Data Type="available" Boolean="true" />
                <Data Type="createTime" Time="current"/>
                <Data Type="editTime" Time="current"/>
                <Data Type="publishTime">
                     <xsl:attribute name="Time"><xsl:value-of select="@Created" /></xsl:attribute>
                </Data>
                <Data Type="deleted" Boolean="false" />
                <Data Type="inheritedAccessControl" Item="{generate-id(..)}" />
                <Data Type="name">
                  <xsl:attribute name="String"><xsl:value-of select="@Name" /></xsl:attribute>
                </Data>
                <Data Type="revision" Number="0" />
                <xsl:choose>
                  <xsl:when test="@StartDate">
                    <Data Type="startTime">
                       <xsl:attribute name="Time"><xsl:value-of select="@StartTime" /></xsl:attribute>
                    </Data>
                  </xsl:when>
                  <xsl:otherwise>
                    <Data Type="startTime" Time="min" />
                  </xsl:otherwise>
                </xsl:choose>
                <xsl:choose>
                  <xsl:when test="@EndDate">
                    <Data Type="stopTime">
                       <xsl:attribute name="Time"><xsl:value-of select="@StopTime" /></xsl:attribute>
                    </Data>
                  </xsl:when>
                  <xsl:otherwise>
                    <Data Type="stopTime" Time="max" />
                  </xsl:otherwise>
                </xsl:choose>
                <xsl:for-each select="cp:Attachment">
                  <Item Id="attachment-{generate-id()}" Type="Attachment">
                    <xsl:attribute name="Attachment">attach/<xsl:value-of select="@FileName" /></xsl:attribute>
                    <xsl:attribute name="URL">/<xsl:value-of select="@FileName" /></xsl:attribute>
                    <Data Type="Attachment.disposition" String="inline" />
                    <Data Type="Attachment.fileName">
                      <xsl:attribute name="String"><xsl:value-of select="@FileName" /></xsl:attribute>
                    </Data>
                    <Data Type="Import.id">
                      <xsl:attribute name="String">
                        <xsl:value-of select="@Id" />
                      </xsl:attribute>
                    </Data>
                    <Data Type="inheritedAccessControl" Item="episode-{generate-id(..)}" />
                  </Item>
                </xsl:for-each>
              </Item>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>
            <xsl:for-each select="cp:Pages/cp:Page">
              <Item Id="{generate-id()}">
                <xsl:attribute name="URL">/<xsl:value-of select="@Title" /></xsl:attribute>
                <xsl:choose>
                  <xsl:when test="$type = 'Wiki'">
                    <xsl:attribute name="Type">WikiPage</xsl:attribute>
                    <Data Type="archetype" String="@template-basicWiki" />
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:if test="$type = 'Blog'">
                      <xsl:attribute name="Type">BlogEntry</xsl:attribute>
                      <Data Type="archetype" String="@template-basicBlog" />
                    </xsl:if>
                    <xsl:if test="$type = 'Journal'">
                      <xsl:attribute name="Type">BlogEntry</xsl:attribute>
                      <Data Type="archetype" String="@template-basicJournal" />
                    </xsl:if>
                  </xsl:otherwise>
                </xsl:choose>
                <Data Type="Import.id">
                  <xsl:attribute name="String">
                    <xsl:value-of select="@Id" />
                  </xsl:attribute>
                </Data>
                <Data Type="uiContext" Item="{generate-id(../..)}" />
                <Data Type="iconName">
                  <xsl:attribute name="String">
                    <xsl:value-of select="$type" />
                  </xsl:attribute>
                </Data>
                <Data Type="inheritedAccessControl" Item="{generate-id(../..)}" />
                <Data Type="deleted">
                  <xsl:attribute name="Boolean">
                    <xsl:value-of select="@Deleted = 'true'" />
                  </xsl:attribute>
                </Data>
                <xsl:if test="$type = 'Wiki'">
                  <Data Type="locked" Boolean="false" />
                  <Data Type="Hierarchy.parent" Item="{generate-id(../..)}" />
                  <Data Type="Hierarchy.index" Number="{count(preceding-sibling::cp:Page)}" />
                </xsl:if>
                <Data Type="revision" Number="{count(cp:Revisions/cp:Revision)}" />
                <xsl:if test="@Creator != 'Unknown User'">
                  <Data Type="creator">
                    <xsl:attribute name="Item">//Integration[@uniqueId='<xsl:value-of select="@Creator" />']/ancestor::User</xsl:attribute>
                  </Data>
                </xsl:if>
                <Data Type="createTime">
                  <xsl:attribute name="Time">
                    <xsl:value-of select="@Created" />
                  </xsl:attribute>
                </Data>
                <xsl:if test="((@Edited != '') and (@Editor != 'Unknown User')) or ((@Edited = '') and (@Creator != 'Unknown User'))">
                  <Data Type="editor">
                    <xsl:attribute name="Item">
                      <xsl:choose>
                        <xsl:when test="@Edited = ''">//Integration[@uniqueId='<xsl:value-of select="@Creator" />']/ancestor::User</xsl:when>
                        <xsl:otherwise>//Integration[@uniqueId='<xsl:value-of select="@Editor" />']/ancestor::User</xsl:otherwise>
                      </xsl:choose>
                    </xsl:attribute>
                  </Data>
                </xsl:if>
                <Data Type="editTime">
                  <xsl:attribute name="Time">
                    <xsl:choose>
                      <xsl:when test="@Edited = ''">
                        <xsl:value-of select="@Created" />
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:value-of select="@Edited" />
                      </xsl:otherwise>
                    </xsl:choose>
                  </xsl:attribute>
                </Data>
                <Data Type="name">
                  <xsl:attribute name="String">
                    <xsl:value-of select="@Title" />
                  </xsl:attribute>
                </Data>
                <xsl:if test="$type = 'Journal' ">
                  <Data Type="announcement">
                    <xsl:attribute name="Boolean">
                      <xsl:value-of select="@Announcement" />
                    </xsl:attribute>
                  </Data>
                </xsl:if>
                <xsl:if test="$type = 'Blog' or $type = 'Journal' ">
                  <Data Type="publishTime">
                    <xsl:attribute name="Time">
                      <xsl:value-of select="@Created" />
                    </xsl:attribute>
                  </Data>
                </xsl:if>
                <Data Type="body">
                  <xsl:value-of select="cp:Body" />
                </Data>
                <xsl:for-each select="cp:Comments/cp:Comment">
                  <Item Id="comment-{generate-id()}" Type="Comment">
                    <xsl:attribute name="URL">Comment</xsl:attribute>
                    <Data Type="Hierarchy.parent" Item="{generate-id(../..)}" />
                    <Data Type="Hierarchy.index" Number="{count(preceding-sibling::cp:Comment)}" />
                    <Data Type="inheritedAccessControl" Item="//Feature[@item='{generate-id(../../..)}']/comments" />
                    <Data Type="feature" Item="//Feature[@item='{generate-id(../../..)}']/comments" />
                    <Data Type="item" Item="{generate-id(../..)}" />
                    <Data Type="name" String="Comment"/>
                    <Data Type="body">
                      <xsl:value-of select="text()" />
                    </Data>
                    <xsl:if test="@Author != 'Unknown User'">
                      <Data Type="creator">
                        <xsl:attribute name="Item">//Integration[@uniqueId='<xsl:value-of select="@Author" />']/ancestor::User</xsl:attribute>
                      </Data>
                    </xsl:if>
                    <Data Type="createTime">
                      <xsl:attribute name="Time">
                        <xsl:value-of select="@Date" />
                      </xsl:attribute>
                    </Data>
                  </Item>
                </xsl:for-each>
                <xsl:for-each select="cp:Attachments/cp:Attachment">
                  <Item Type="Attachment">
                    <xsl:attribute name="URL">/<xsl:value-of select="@FileName" /></xsl:attribute>
                    <xsl:attribute name="Attachment">attach/<xsl:value-of select="@FileName" /></xsl:attribute>
                    <Data Type="Attachment.fileName">
                      <xsl:attribute name="String">
                        <xsl:value-of select="@FileName" />
                      </xsl:attribute>
                    </Data>
                    <Data Type="Import.id">
                      <xsl:attribute name="String">
                        <xsl:value-of select="@Id" />
                      </xsl:attribute>
                    </Data>
                    <Data Type="inheritedAccessControl" Item="{generate-id(../../../..)}" />
                  </Item>
                </xsl:for-each>
                <xsl:for-each select="cp:Revisions/cp:Revision">
                  <Item Id="{generate-id()}" Type="Revision">
                    <Data Type="body">
                      <xsl:value-of select="text()" />
                    </Data>
                    <xsl:if test="@Author != 'Unknown User'">
                      <Data Type="editor">
                        <xsl:attribute name="Item">//Integration[@uniqueId='<xsl:value-of select="@Author" />']/ancestor::User</xsl:attribute>
                      </Data>
                    </xsl:if>
                    <Data Type="editTime">
                      <xsl:attribute name="Time">
                        <xsl:value-of select="@Date" />
                      </xsl:attribute>
                    </Data>
                    <Data Type="name">
                      <xsl:attribute name="String">
                        <xsl:value-of select="../../@Title" />
                      </xsl:attribute>
                    </Data>
                    <Data Type="revision">
                      <xsl:attribute name="Number">
                        <xsl:value-of select="@Number" />
                      </xsl:attribute>
                    </Data>
                  </Item>
                </xsl:for-each>
              </Item>
            </xsl:for-each>
          </xsl:otherwise>
        </xsl:choose>
      </Item>
    </Dump>
  </xsl:template>
</xsl:stylesheet>

