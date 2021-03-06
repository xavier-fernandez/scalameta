package scala.meta.tests
package scalahost

import scala.meta._
import scala.meta.internal.semantic.SemanticdbMode

class SemanticSuite extends DatabaseSuite(SemanticdbMode.Slim) {
  names(
    """
    |object First {
    |  def main(args: Array[String]): Unit = {
    |    val list = List(1, 2, 3)
    |    println(list)
    |  }
    |}
  """.trim.stripMargin,
    """
    |[7..12): First => _empty_.First.
    |[21..25): main => _empty_.First.main([Ljava/lang/String;)V.
    |[26..30): args => _empty_.First.main([Ljava/lang/String;)V.(args)
    |[32..37): Array => _root_.scala.Array#
    |[38..44): String => _root_.scala.Predef.String#
    |[48..52): Unit => _root_.scala.Unit#
    |[65..69): list => <...>@61..85
    |[72..76): List => _root_.scala.collection.immutable.List.
    |[90..97): println => _root_.scala.Predef.println(Ljava/lang/Object;)V.
    |[98..102): list => <...>@61..85
  """.trim.stripMargin
  )

  targeted(
    """
    |object <<Second>> {
    |  def doSomething = {
    |    42
    |  }
    |}
  """.trim.stripMargin, { implicit database => second =>
      assert(second.symbol === Symbol("_empty_.Second."))
    }
  )

  names(
    """
    |import _root_.scala.List
    |
    |class C {
    |  _root_.scala.List
    |}
  """.trim.stripMargin,
    """
    |[7..13): _root_ => _root_.
    |[14..19): scala => _root_.scala.
    |[20..24): List => _root_.scala.package.List.;_root_.scala.package.List#
    |[32..33): C => _empty_.C#
    |[38..44): _root_ => _root_.
    |[45..50): scala => _root_.scala.
    |[51..55): List => _root_.scala.collection.immutable.
  """.trim.stripMargin
  )

  targeted(
    // curried function application with named args, #648
    """
      |object Foo {
      |  def bar(children: Int)(x: Int) = children + x
      |  <<bar>>(children = 4)(3)
      |}
    """.trim.stripMargin, { implicit database => second =>
      assert(second.symbol === Symbol("_empty_.Foo.bar(II)I."))
    }
  )

  targeted(
    """
      |case class User(name: String, age: Int)
      |object Library {
      |  val u: User = ???
      |  u.<<copy>>(<<age>> = 43)
      |}
    """.trim.stripMargin, { implicit database => (copy, age) =>
      assert(copy.symbol === Symbol("_empty_.User#copy(Ljava/lang/String;I)LUser;."))
      assert(age.symbol === Symbol("_empty_.User#copy(Ljava/lang/String;I)LUser;.(age)"))
    }
  )

  denotations(
    """
      |import scala.language.experimental.macros
      |
      |package foo {
      |  class C1(p1: Int, val p2: Int, var p3: Int) {
      |    def this() = this(0, 0, 0)
      |    val f1 = {
      |      val l1 = ???
      |      var l2 = ???
      |      ???
      |    }
      |    var f2 = ???
      |    def m1[T](x: Int): Int = ???
      |    def m2 = macro ???
      |    type T1 <: Int
      |    type T2 = Int
      |  }
      |
      |  abstract class C2 {
      |    def m3: Int
      |    final def m4 = ???
      |  }
      |
      |  sealed class C3 extends C2 {
      |    def m3 = 42
      |    override def toString = ""
      |  }
      |
      |  trait T {
      |    private val f1 = ???
      |    private[this] val f2 = ???
      |    private[foo] val f3 = ???
      |    protected var f4 = ???
      |    protected[this] var f5 = ???
      |    protected[foo] var f6 = ???
      |  }
      |
      |  object M {
      |    implicit def i1 = ???
      |    lazy val l1 = ???
      |    case class C1()
      |    class C2[+T, -U]
      |  }
      |}
      |
      |package object foo {
      |}
  """.trim.stripMargin,
    """
      |_root_.foo. => package foo
      |_root_.foo.C1# => class C1
      |_root_.foo.C1#(p1) => param p1: Int
      |_root_.foo.C1#(p2) => val param p2: Int
      |_root_.foo.C1#(p3) => var param p3_=: (x$1: Int)Unit
      |_root_.foo.C1#T1# => abstract type T1:  <: Int
      |_root_.foo.C1#T2# => type T2: Int
      |_root_.foo.C1#`<init>`()V. => secondaryctor <init>: ()foo.C1
      |_root_.foo.C1#`<init>`(III)V. => primaryctor <init>: (p1: Int, p2: Int, p3: Int)foo.C1
      |_root_.foo.C1#f1. => val f1: Nothing
      |_root_.foo.C1#f1.l1. => val l1: Nothing
      |_root_.foo.C1#f1.l2. => var l2: Nothing
      |_root_.foo.C1#f2. => var f2_=: (x$1: Nothing)Unit
      |_root_.foo.C1#m1(I)I. => def m1: [T](x: Int)Int
      |_root_.foo.C1#m1(I)I.(x) => param x: Int
      |_root_.foo.C1#m1(I)I.T# => typeparam T
      |_root_.foo.C1#m2()Lscala/Nothing;. => macro m2: Nothing
      |_root_.foo.C2# => abstract class C2
      |_root_.foo.C2#`<init>`()V. => primaryctor <init>: ()foo.C2
      |_root_.foo.C2#m3()I. => abstract def m3: Int
      |_root_.foo.C2#m4()Lscala/Nothing;. => final def m4: Nothing
      |_root_.foo.C3# => sealed class C3
      |_root_.foo.C3#`<init>`()V. => primaryctor <init>: ()foo.C3
      |_root_.foo.C3#m3()I. => def m3: Int
      |_root_.foo.C3#toString()Ljava/lang/String;. => def toString: ()String
      |_root_.foo.M. => final object M
      |_root_.foo.M.C1# => case class C1
      |_root_.foo.M.C1#`<init>`()V. => primaryctor <init>: ()foo.M.C1
      |_root_.foo.M.C2# => class C2
      |_root_.foo.M.C2#[T] => covariant typeparam T
      |_root_.foo.M.C2#[U] => contravariant typeparam U
      |_root_.foo.M.C2#`<init>`()V. => primaryctor <init>: ()foo.M.C2[T,U]
      |_root_.foo.M.i1()Lscala/Nothing;. => implicit def i1: Nothing
      |_root_.foo.M.l1. => lazy val l1: Nothing
      |_root_.foo.T# => trait T
      |_root_.foo.T#f1. => private val f1: Nothing
      |_root_.foo.T#f2. => private val f2: Nothing
      |_root_.foo.T#f3. => private val f3: Nothing
      |_root_.foo.T#f4. => protected var f4_=: (x$1: Nothing)Unit
      |_root_.foo.T#f5. => protected var f5_=: (x$1: Nothing)Unit
      |_root_.foo.T#f6. => protected var f6_=: (x$1: Nothing)Unit
      |_root_.foo.package. => packageobject package
      |_root_.scala. => package scala
      |_root_.scala.Int# => abstract final class Int
      |_root_.scala.Int#`<init>`()V. => primaryctor <init>: ()Int
      |_root_.scala.Predef.`???`()Lscala/Nothing;. => def ???: Nothing
      |_root_.scala.language. => final object language
      |_root_.scala.language.experimental. => final object experimental
      |_root_.scala.language.experimental.macros. => implicit lazy val macros: languageFeature.experimental.macros
  """.trim.stripMargin
  )

  sugars(
    """
      |package commandeer
      |import scala.language.higherKinds
      |trait CommandeerDSL[Host]
      |object CommandeerDSL {
      |  def apply[Host, DSL <: CommandeerDSL[Host]](host: Host)(implicit dsl: DSL): DSL = dsl
      |}
      |trait Foo
      |object Foo {
      |  implicit val fooDSL: FooDSL = new FooDSL {}
      |}
      |trait FooDSL extends CommandeerDSL[Foo]
      |object RunMe {
      |  CommandeerDSL(null.asInstanceOf[Foo])
      |}
  """.trim.stripMargin,
    """
      |[333..333) [commandeer.Foo, commandeer.FooDSL]
      |[357..357) (commandeer.Foo.fooDSL)
  """.trim.stripMargin
  )

  sugars(
    """
      |class C[T]
      |object C {
      |  implicit def int: C[Int] = new C[Int]
      |  implicit def list[T: C]: C[List[T]] = ???
      |}
      |
      |class X
      |object X {
      |  implicit def cvt[T: C](x: T): X = ???
      |}
      |
      |object M {
      |  Nil.map(x => 2)
      |
      |  def c[T: C] = ???
      |  M.c[List[Int]]
      |
      |  def x(x: X) = ???
      |  x(42)
      |
      |  def i[T](t: T) = ???
      |  i(new C[Int])
      |}
  """.trim.stripMargin,
    """
      |[191..191) [Int, List[Int]]
      |[199..199) (scala.collection.immutable.List.canBuildFrom[Int])
      |[237..237) (C.list[Int](C.int))
      |[263..265) X.cvt[Int](*)(C.int)
      |[294..294) [C[Int]]
  """.trim.stripMargin
  )

  denotations(
    """
      |import scala.collection.mutable.ListBuffer
      |import scala.collection.mutable.{HashSet => HS}
      |trait B {
      |  type X
      |  def x: X
      |}
      |class E extends B {
      |  type X = ListBuffer[Int]
      |  def x = ListBuffer.empty
      |}
      |class D extends B {
      |  type X = HS[Int]
      |  def x = HS.empty
      |}
      |object a {
      |  val x = new E().x
      |  val y = new D().x
      |  def foo(b: B): b.X = {
      |    val result = b.x
      |    result
      |  }
      |}
    """.stripMargin,
    """
      |<...>@340..356 => val result: b.X
      |_empty_.B# => trait B
      |_empty_.B#X# => abstract type X
      |_empty_.B#x()Ljava/lang/Object;. => abstract def x: B.this.X
      |_empty_.D# => class D
      |_empty_.D#X# => type X: scala.collection.mutable.HashSet[Int]
      |_empty_.D#`<init>`()V. => primaryctor <init>: ()D
      |_empty_.D#x()Lscala/collection/mutable/HashSet;. => def x: scala.collection.mutable.HashSet[Int]
      |_empty_.E# => class E
      |_empty_.E#X# => type X: scala.collection.mutable.ListBuffer[Int]
      |_empty_.E#`<init>`()V. => primaryctor <init>: ()E
      |_empty_.E#x()Lscala/collection/mutable/ListBuffer;. => def x: scala.collection.mutable.ListBuffer[Int]
      |_empty_.a. => final object a
      |_empty_.a.foo(LB;)Ljava/lang/Object;. => def foo: (b: B)b.X
      |_empty_.a.foo(LB;)Ljava/lang/Object;.(b) => param b: B
      |_empty_.a.x. => val x: scala.collection.mutable.ListBuffer[Int]
      |_empty_.a.y. => val y: scala.collection.mutable.HashSet[Int]
      |_root_.scala. => package scala
      |_root_.scala.Int# => abstract final class Int
      |_root_.scala.Int#`<init>`()V. => primaryctor <init>: ()Int
      |_root_.scala.collection. => package collection
      |_root_.scala.collection.generic.GenericCompanion#empty()Lscala/collection/GenTraversable;. => def empty: [A]=> CC[A]
      |_root_.scala.collection.mutable. => package mutable
      |_root_.scala.collection.mutable.HashSet# => class HashSet
      |_root_.scala.collection.mutable.HashSet#`<init>`(Lscala/collection/mutable/FlatHashTable/Contents;)V. => private primaryctor <init>: (contents: scala.collection.mutable.FlatHashTable.Contents[A])scala.collection.mutable.HashSet[A]
      |_root_.scala.collection.mutable.HashSet. => final object HashSet
      |_root_.scala.collection.mutable.HashSet.;_root_.scala.collection.mutable.HashSet# => val <import scala.collection.mutable.HashSet>: scala.collection.mutable.HashSet.type <and> scala.collection.mutable.HashSet
      |_root_.scala.collection.mutable.HashSet.empty()Lscala/collection/mutable/HashSet;. => def empty: [A]=> scala.collection.mutable.HashSet[A]
      |_root_.scala.collection.mutable.ListBuffer# => final class ListBuffer
      |_root_.scala.collection.mutable.ListBuffer#`<init>`()V. => primaryctor <init>: ()scala.collection.mutable.ListBuffer[A]
      |_root_.scala.collection.mutable.ListBuffer. => final object ListBuffer
      |_root_.scala.collection.mutable.ListBuffer.;_root_.scala.collection.mutable.ListBuffer# => val <import scala.collection.mutable.ListBuffer>: scala.collection.mutable.ListBuffer.type <and> scala.collection.mutable.ListBuffer
    """.stripMargin.trim
  )

  sugars(
    "class F[T: Manifest] { val arr = Array.empty[T] }",
    "[47..47) (F.this.evidence$1)".trim
  )

  names(
    s"""
       |object tup {
       |  val foo = (a: (Int, Boolean)) => 1
       |  foo(2, true)
       |  foo.apply(2, true)
       |}
    """.stripMargin,
    """
      |[8..11): tup => _empty_.tup.
      |[20..23): foo => _empty_.tup.foo.
      |[27..28): a => _empty_.tup.foo.$anonfun.(a)
      |[31..34): Int => _root_.scala.Int#
      |[36..43): Boolean => _root_.scala.Boolean#
      |[53..56): foo => _empty_.tup.foo.
      |[68..71): foo => _empty_.tup.foo.
      |[72..77): apply => _root_.scala.Function1#apply(Ljava/lang/Object;)Ljava/lang/Object;.
    """.stripMargin.trim
  )

  messages(
    """
      |import scala.collection.mutable. /* comment */{ Map, Set, ListBuffer }
      |import scala.concurrent._, collection.mutable.{HashSet, Buffer}
      |import scala.collection.{ /* comment */mutable /* comment */ => m}
      |object a {
      |  ListBuffer.empty[Int]
      |  HashSet.empty[Int]
      |}
    """.stripMargin.trim,
    """
      |[48..51): [warning] Unused import
      |[53..56): [warning] Unused import
      |[71..96): [warning] Unused import
      |[127..133): [warning] Unused import
      |[174..181): [warning] Unused import
    """.stripMargin.trim
  )

  denotations(
    """
      |object A {
      |  List.newBuilder[Int].result
      |  List(1).head
      |}""".stripMargin,
    """
      |_empty_.A. => final object A
      |_root_.scala.Int# => abstract final class Int
      |_root_.scala.Int#`<init>`()V. => primaryctor <init>: ()Int
      |_root_.scala.collection.IterableLike#head()Ljava/lang/Object;. => def head: A
      |_root_.scala.collection.immutable.List. => final object List
      |_root_.scala.collection.immutable.List.newBuilder()Lscala/collection/mutable/Builder;. => def newBuilder: [A]=> scala.collection.mutable.Builder[A,List[A]]
      |_root_.scala.collection.mutable.Builder#result()Ljava/lang/Object;. => abstract def result: ()To
    """.stripMargin.trim
  )
}
