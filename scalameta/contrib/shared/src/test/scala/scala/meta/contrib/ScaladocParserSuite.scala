package scala.meta.contrib

import scala.meta.contrib.DocToken._
import scala.meta.tokens.Token.Comment
import scala.meta.Defn
import scala.meta._

import org.scalatest.FunSuite

/**
  * Test for [[ScaladocParser]]
  */
class ScaladocParserSuite extends FunSuite {

  private[this] def parseString(commentCode: String): Option[Seq[DocToken]] = {
    val code = commentCode.parse[Source].get
    val comments = AssociatedComments(code.tokens)
    val defnClass = code.collectFirst { case t: Defn.Class => t }.get
    val comment: Comment = comments.leading(defnClass).head
    ScaladocParser.parseScaladoc(comment)
  }

  private[this] def generateTestString(docKind: TagKind): String =
    s"${docKind.label} ${(0 until docKind.numberParameters).map(i => s"Test$docKind$i").mkString(" ")}"

  test("example usage") {
    assert(
      parseString(
        """
          | /** Example scaladoc **/
          | case class foo(bar: String)
        """.stripMargin
      ).toString === "Some(List(Description(Example scaladoc)))"
    )
  }

  test("indentation checks") {

    val expectedBody: String = "BODY"
    val expectedResult: Option[Seq[DocToken]] = Option(Seq(DocToken(Description, expectedBody)))

    assert(
      parseString(
        s"""
         /** $expectedBody*/
          case class foo(bar: String)
         """
      ) === expectedResult
    )
    assert(
      parseString(
        s"""
         /** $expectedBody
          */
          case class foo(bar: String)
         """
      ) === expectedResult
    )
    assert(
      parseString(
        s"""
         /**       $expectedBody
          */
          case class foo(bar: String)
         """
      ) === expectedResult
    )
    assert(
      parseString(
        s"""
         /**
          *$expectedBody
          */
          case class foo(bar: String)
         """
      ) === expectedResult
    )
  }

  test("paragraph parsing") {
    val descriptionBody = "Description Body"
    assert(
      parseString(
        s"""
         /**
          *
          *$descriptionBody
          *
          *$descriptionBody
          *
          */
          case class foo(bar: String)
         """
      ) === Option(
        Seq(
          DocToken(Description, descriptionBody),
          DocToken(Paragraph),
          DocToken(Description, descriptionBody)
        )
      )
    )
  }

  test("code blocks") {

    val testDescription = "This is a codeblock:"

    val codeBlock1 = "\"HELLO MARIANO\""
    val codeBlock2 = "\"HELLO SORAYA\""
    val complexCodeBlock =
      """
        |ggmqwogmwogmqwomgq
        |val x = 1 // sdfdfh
        |// zzz
        |gmqwgoiqmgoqmwomw
      """.stripMargin.trim

    val result: Option[Seq[DocToken]] =
      parseString(
        s"""
          /**
            * $testDescription {{{ $codeBlock1 }}}
            * $testDescription
            * {{{ $codeBlock2 }}}
            *
            * $testDescription
            *
            * {{{
            *   $complexCodeBlock
            * }}}
            */
            case class foo(bar : String)
       """.stripMargin
      )

    val expectation = Option(
      Seq(
        DocToken(Description, testDescription),
        DocToken(CodeBlock, codeBlock1),
        DocToken(Description, testDescription),
        DocToken(CodeBlock, codeBlock2),
        DocToken(Paragraph),
        DocToken(Description, testDescription),
        DocToken(Paragraph),
        DocToken(CodeBlock, complexCodeBlock)
      )
    )
    assert(result === expectation)
  }

  test("headings") {
    val headingBody = "Overview"
    val subHeadingBody = "Of the heading"

    val result: Option[Seq[DocToken]] =
      parseString(
        s"""
        /**
          * =$headingBody=
          * ==$subHeadingBody==
          */
         case class foo(bar : String)
         """
      )
    val expectation = Option(
      Seq(
        DocToken(Heading, headingBody),
        DocToken(SubHeading, subHeadingBody)
      )
    )

    assert(result === expectation)
  }

  test("label parsing/merging") {
    val testStringToMerge = "Test DocText"
    val scaladoc: String =
      DocToken.tagTokenKinds
        .flatMap(token => Seq(generateTestString(token), testStringToMerge))
        .mkString("/**\n * ", "\n * ", "\n */")

    val codeToParse: String =
      s"""
         |$scaladoc
         |case class Foo(bar: String)
      """.stripMargin

    val parsedScaladoc: Option[Seq[DocToken]] = parseString(codeToParse)

    // Inherit doc does not merge
    assert(parsedScaladoc.map(_.size) === Option(DocToken.tagTokenKinds.size))

    // Inherit doc does not merge
    assert(
      parsedScaladoc
        .exists(
          _.filterNot(_.kind == DocToken.InheritDoc)
            .forall(_.body.getOrElse("").endsWith(testStringToMerge))
        )
    )
  }

  test("references") {
    // Scaladoc without references
    assert(
      parseString(
        """
          /** Example scaladoc **/
          case class foo(bar: String)
        """.stripMargin
      ).exists(_.head.references === Nil)
    )
    // Scaladoc with references
    val reference1 = "Scala.some"
    val reference2 = "java.util.Random"

    val codeToParse: String =
      s"""
         |/**
         | * Random description with references [[$reference1]] and [[$reference2]].
         | */
         |case class Foo(bar: String) extends AnyVal
      """.stripMargin

    assert(
      parseString(codeToParse).exists(
        _.head.references === Seq(DocToken.Reference(reference1), DocToken.Reference(reference2))
      )
    )
  }
}
