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

<!-- TODO input type="image" -->

<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:fn="http://www.w3.org/2005/xpath-functions"
               xmlns:wb="http://learningobjects.com/schema/ug/1.0/workbook/lesson"
               exclude-result-prefixes="fn"
               version="2.0">

    <!-- wrap lesson components within a common element to faciliate sorting -->

    <xsl:template match="Lesson/Question">
        <xsl:call-template name="lessonComponent"/>
    </xsl:template>

    <xsl:template match="Lesson/Paragraph">
        <xsl:call-template name="lessonComponent"/>
    </xsl:template>

    <xsl:template match="Exercise">
        <xsl:call-template name="lessonComponent"/>
    </xsl:template>

    <xsl:template match="Exercise/Question">
        <xsl:call-template name="exerciseComponent"/>
    </xsl:template>

    <xsl:template match="Exercise/Paragraph">
        <xsl:call-template name="exerciseComponent"/>
    </xsl:template>

    <!-- write out fill in the blank meta data -->
    <xsl:template match="Question[@type='FillInTheBlank']" priority="1">
        <wb:ExerciseComponent>
            <wb:Question>
                <xsl:copy-of select="@*" />
                <xsl:apply-templates />
                <wb:FillInTheBlank>
                    <xsl:for-each select=".//input[@type='fill-in-the-blank']">
                        <wb:blank id="{generate-id(.)}">
                            <xsl:apply-templates />
                        </wb:blank>
                    </xsl:for-each>
                </wb:FillInTheBlank>
            </wb:Question>
        </wb:ExerciseComponent>
    </xsl:template>

    <xsl:template match="Question[@type='FillInTheBlank']//input" priority="1">
        <input id="{generate-id(.)}" />
    </xsl:template>

    <!-- specify ordinates and coordinates as attributes instead of as part of the element name -->
    <xsl:template match="*[matches(name(), 'html-title-\d+$')]">
        <xsl:variable name="tokens" select="fn:tokenize(name(), '-')"/>
        <wb:html-title>
            <xsl:attribute name="index">
                <xsl:value-of select="$tokens[3]"/>
            </xsl:attribute>
            <xsl:apply-templates />
        </wb:html-title>
    </xsl:template>

    <xsl:template match="*[matches(name(), 'html-value-\d+-\d+$')]">
        <xsl:variable name="tokens" select="fn:tokenize(name(),'-')"/>
        <wb:html-value>
            <xsl:attribute name="x">
                <xsl:value-of select="$tokens[3]"/>
            </xsl:attribute>
            <xsl:attribute name="y">
                <xsl:value-of select="$tokens[4]"/>
            </xsl:attribute>
            <xsl:apply-templates />
        </wb:html-value>
    </xsl:template>

    <xsl:template match="*[matches(name(), 'html-value-\d+$')]">
        <xsl:variable name="tokens" select="fn:tokenize(name(),'-')"/>
        <wb:html-value>
            <xsl:attribute name="index">
                <xsl:value-of select="$tokens[3]"/>
            </xsl:attribute>
            <xsl:apply-templates />
        </wb:html-value>
    </xsl:template>

    <xsl:template match="*[matches(name(), 'html-right-\d+$')]">
        <xsl:variable name="tokens" select="fn:tokenize(name(),'-')"/>
        <wb:html-right>
            <xsl:attribute name="index">
                <xsl:value-of select="$tokens[3]"/>
            </xsl:attribute>
            <xsl:apply-templates />
        </wb:html-right>
    </xsl:template>

    <xsl:template match="*[matches(name(), 'html-left-\d+$')]">
        <xsl:variable name="tokens" select="fn:tokenize(name(),'-')"/>
        <wb:html-left>
            <xsl:attribute name="index">
                <xsl:value-of select="$tokens[3]"/>
            </xsl:attribute>
            <xsl:apply-templates />
        </wb:html-left>
    </xsl:template>

    <!-- specify ordinated attributes as elements with attributes -->

    <xsl:template match="Match">
        <wb:Match>
            <xsl:for-each select="@*[matches(name(), 'answer-\d+$')]">
                <xsl:call-template name="answer" />
            </xsl:for-each>
            <xsl:apply-templates />
        </wb:Match>
    </xsl:template>

    <xsl:template match="TrueFalse">
        <wb:TrueFalse>
            <xsl:for-each select="@*[matches(name(), 'answer-\d+$')]">
                <xsl:call-template name="answer" />
            </xsl:for-each>
            <xsl:apply-templates />
        </wb:TrueFalse>
    </xsl:template>

    <xsl:template match="CheckBox">
        <wb:CheckBox>
            <xsl:for-each select="@*[matches(name(), 'checked-\d+$')]">
                <xsl:call-template name="answer" />
            </xsl:for-each>
            <xsl:apply-templates />
        </wb:CheckBox>
    </xsl:template>

    <!-- write out table attributes as elements with coordinate attributes -->
    <xsl:template match="Table">
        <wb:Table>
            <xsl:for-each select="@*[matches(name(), 'column-line-\d+$')]">
              <xsl:variable name="index" select="fn:tokenize(name(), '-')[3]"/>
              <wb:column>
                  <xsl:attribute name="index">
                      <xsl:value-of select="$index" />
                  </xsl:attribute>
                  <xsl:attribute name="line">
                      <xsl:value-of select="." />
                  </xsl:attribute>
                  <xsl:attribute name="weight">
                      <xsl:value-of select="../@*[name()=concat('column-weight-', $index)]" />
                  </xsl:attribute>
              </wb:column>
            </xsl:for-each>
            <xsl:for-each select="@*[matches(name(), 'row-line-\d+$')]">
              <xsl:variable name="index" select="fn:tokenize(name(), '-')[3]"/>
              <wb:row>
                  <xsl:attribute name="index">
                      <xsl:value-of select="$index" />
                  </xsl:attribute>
                  <xsl:attribute name="line">
                      <xsl:value-of select="." />
                  </xsl:attribute>
              </wb:row>
            </xsl:for-each>
            <xsl:for-each select="html-value">
                <xsl:variable name="column" select="@index0"/>
                <xsl:variable name="row" select="@index1"/>
                <xsl:variable name="type" select="../@*[name()=concat(concat(concat('type-', $column), '-'), $row)]"/>
                <wb:cell column="{$column}" row="{$row}">
                    <xsl:attribute name="type">
                        <xsl:choose>
                            <xsl:when test="$type and $type != ''">
                                <xsl:value-of select="$type" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:text>text</xsl:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <xsl:apply-templates />
                </wb:cell>
            </xsl:for-each>
            <xsl:for-each select="@*[matches(name(), 'type-\d+-\d+$')]">
                <xsl:variable name="column" select="fn:tokenize(name(), '-')[2]" />
                <xsl:variable name="row" select="fn:tokenize(name(), '-')[3]" />
                <wb:cell>
                    <xsl:attribute name="column">
                        <xsl:value-of select="$column" />
                    </xsl:attribute>
                    <xsl:attribute name="row">
                        <xsl:value-of select="$row" />
                    </xsl:attribute>
                    <xsl:attribute name="type">
                        <xsl:value-of select="." />
                    </xsl:attribute>
                    <xsl:for-each select="../*[name()=concat(concat(concat('html-value-', $column), '-'), $row)]">
                        <xsl:apply-templates />
                    </xsl:for-each>
                    <xsl:for-each select="../html-value[@index0=$column][@index1=$row]">
                        <xsl:apply-templates />
                    </xsl:for-each>
                </wb:cell>
            </xsl:for-each>
        </wb:Table>
    </xsl:template>

    <!-- image placeholders -->

    <xsl:template match="input[(@type = 'image')]" priority="2">
      <input type="image" src="{@src}" alt="Image ({@src})" />
      <div><xsl:value-of select="html-caption" /></div>
    </xsl:template>

    <!-- omit namespace on body innards so they can be treated as plain HTML-->

    <xsl:template match="body/*">
        <xsl:call-template name="identity"/>
    </xsl:template>

    <xsl:template match="p/*">
        <xsl:call-template name="identity"/>
    </xsl:template>

    <!-- copy everything else as is -->

    <xsl:template match="*">
        <xsl:call-template name="namespacedIdentity"/>
    </xsl:template>

    <!-- named templates -->

    <xsl:template name="namespacedIdentity">
        <xsl:element name="wb:{local-name()}">
          <xsl:copy-of select="@*" />
          <xsl:apply-templates />
        </xsl:element>
    </xsl:template>

    <xsl:template name="identity">
        <xsl:copy>
            <xsl:copy-of select="@*" />
            <xsl:apply-templates />
        </xsl:copy>
    </xsl:template>

    <xsl:template name="lessonComponent">
        <wb:LessonComponent>
            <xsl:call-template name="namespacedIdentity" />
        </wb:LessonComponent>
    </xsl:template>

    <xsl:template name="exerciseComponent">
        <wb:ExerciseComponent>
            <xsl:call-template name="namespacedIdentity" />
        </wb:ExerciseComponent>
    </xsl:template>

    <xsl:template name="answer">
      <wb:answer>
          <xsl:attribute name="index">
              <xsl:value-of select="fn:tokenize(name(), '-')[2]" />
          </xsl:attribute>
          <xsl:attribute name="value">
              <xsl:value-of select="." />
          </xsl:attribute>
      </wb:answer>
    </xsl:template>

</xsl:transform>
