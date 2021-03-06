package tina

import java.util.Objects

/**
  * Created by Lin on 17/4/28.
  */
case class TinaCompiler() {

}

case class TinaToken(name: Any, kind: Int, line: Int, pos: Int) {

  override def toString: String = "name: " + name + " kind: " + TinaToken.tokenNames(kind) + " in line: " + line + " pos: " + pos
}

case class MisMatchTokenException(e: String) extends RuntimeException(e)

object TinaToken {
  val INT = 0
  val FLOAT = 1
  val LEFT_PARENT = 2
  val RIGHT_PARENT = 3
  val LEFT_BRACE = 4
  val RIGHT_BRACE = 5
  val TINA = 6
  val LOVE = 7
  val STRING = 8
  val ADD = 9
  val SUB = 10
  val MUL = 11
  val DIV = 12
  val WHILE = 13
  val UNIT = 14
  val RETURN = 15
  val FOR = 16
  val IF = 17
  val ELSE = 18
  val ELIF = 19
  val DOUBLE_QUOTATION = 20
  val SINGLE_QUOTATION = 21
  val CLASS = 22
  val COLON = 23
  val VAR = 24
  val EQUAL = 25
  val FUNCTION = 26
  val GREAT = 27
  val LOW = 28
  val COMMA = 29
  val SEMICOLON=30
  val tokenNames: List[String] = List[String](
    "int", "float", "(", ")", "{",
    "}", "tina", "love", "string", "+",
    "-", "*", "/", "while", "unit",
    "return", "for", "if", "else", "elif",
    "\"", "'", "class", ":", "var",
    "=", "function", ">", "<", ",",
    ";"
  )

  def convertType(kind: Int): String = kind match {
    case TinaToken.INT => TinaType.typeNames(TinaType.LITERAL)
    case TinaToken.FLOAT => TinaType.typeNames(TinaType.LITERAL)
    case TinaToken.STRING => TinaType.typeNames(TinaType.LITERAL)
    case TinaToken.VAR => TinaType.typeNames(TinaType.IDENTIFIER)

  }
}

case class TinaLexer(src: Array[Char]) {
  var index: Int = 0
  var lineIndex: Int = 0
  var inferIndexs: List[(Int, Int)] = List[(Int, Int)]()

  var isInfer: Boolean = false
  var isComment: Boolean = false

  var buffer: List[TinaToken] = List[TinaToken]()
  var line: Int = 1

  /**
    * 匹配该token
    *
    * @param kind
    * @return
    */
  def matchToken(kind: Int): TinaToken = {
    val token = nextToken()
    if (token.kind == kind) {
      token
    } else {
      throw MisMatchTokenException("expected " + TinaToken.tokenNames(kind) + " actual " + TinaToken.tokenNames(token.kind))
    }
  }

  /**
    * 判断是否为此token
    *
    * @param kind
    * @return
    */
  def isNextTokenOf(kind: Int): Boolean = {
    val tmp = buffer
    reset()
    syn(1)
    val token = buffer(0)
    if (buffer.length == 1) {
      buffer = tmp
      if (token.kind == kind) true else false
    } else
      throw MisMatchTokenException("expected " + TinaToken.tokenNames(kind) + " actual " + TinaToken.tokenNames(token.kind))
  }

  def skip(): Unit = index match {
    case lt if index < src.length =>
      val char = src(index)
      char match {
        case isSkip if (char == '\t' || char == ' ' || char == '\r') && (!isComment) =>
          index = index + 1
          lineIndex = lineIndex + 1
          skip()

        case isEscaped if char == '\\' =>
          if (index + 2 < src.length)
            index = index + 2
          lineIndex = lineIndex + 2
          skip()

        case isBeginComment if char == '/' && (index + 1) < src.length && src(index + 1) == '*' =>
          index = index + 2
          lineIndex = lineIndex + 2
          isComment = true
          skip()

        case isEndComment if isComment && char == '*' && (index + 1) < src.length && src(index + 1) == '/' =>
          index = index + 2
          lineIndex = lineIndex + 2
          isComment = false
          skip()

        case isInComment if isComment =>
          index = index + 1
          lineIndex = lineIndex + 1
          skip()

        case _ =>
      }

    case _ =>

  }

  def hasNumber(): Boolean = {
    var result = false
    if (index < src.length) {
      val char = src(index)
      char match {
        case isNum if char >= '0' && char <= '9' => result = true
        case _ => result = false
      }
    }
    result
  }

  def hasLetter(): Boolean = {
    var result = false
    if (index < src.length) {
      val char = src(index)
      char match {
        case isTrue if (char >= 'a' && char <= 'z') || (char >= 'A' && char <= 'Z') || char == '_' || (char >= '0' && char <= '9') => result = true
        case _ => result = false
      }
    }
    result
  }

  def nextLetters(token: TinaToken): TinaToken = if (hasLetter()) {
    val name = token.name.asInstanceOf[String] + src(index)
    forward(1)
    if (name == TinaToken.tokenNames(TinaToken.IF)) {
      nextLetters(TinaToken(name, TinaToken.IF, line, token.pos + 1))
    } else if (name == TinaToken.tokenNames(TinaToken.LOVE)) {
      nextLetters(TinaToken(name, TinaToken.LOVE, line, token.pos + 1))
    } else if (name == TinaToken.tokenNames(TinaToken.TINA)) {
      nextLetters(TinaToken(name, TinaToken.TINA, line, token.pos + 1))
    } else if (name == TinaToken.tokenNames(TinaToken.RETURN)) {
      nextLetters(TinaToken(name, TinaToken.RETURN, line, token.pos + 1))
    } else if (index < src.length && src(index).toString == TinaToken.tokenNames(TinaToken.LEFT_PARENT)) {
      nextLetters(TinaToken(name, TinaToken.FUNCTION, line, token.pos + 1))
    }
    else {
      nextLetters(TinaToken(name, TinaToken.VAR, line, token.pos + 1))
    }
  } else token


  def nextNumber(token: TinaToken, dotNum: Int): TinaToken = {
    var result = token
    if (index < src.length) {
      if (src(index) == '.' && token.kind == TinaToken.INT) {
        index = index + 1
        lineIndex = lineIndex + 1
        result = nextNumber(TinaToken(token.name.asInstanceOf[Int].toDouble, TinaToken.FLOAT, token.line, token.pos + 1), dotNum)
      }
      else if (hasNumber()) {
        val int = src(index).toInt - '0'.toInt
        index = index + 1
        lineIndex = lineIndex + 1
        if (token.kind == TinaToken.INT)
          result = nextNumber(TinaToken(token.name.asInstanceOf[Int] * 10 + int, token.kind, token.line, token.pos + 1), dotNum)
        else {

          var dotResult = int.toDouble
          for (i <- 0 to dotNum)
            dotResult = dotResult / 10

          result = nextNumber(TinaToken(token.name.asInstanceOf[Double] + dotResult, token.kind, token.line, token.pos + 1), dotNum + 1)

        }
      }
      else if (src(index - 1) == '.' && token.kind == TinaToken.FLOAT)
        throw MisMatchTokenException("expecting original type is int but found float.Is double dot in this number :) ")
    }
    result
  }

  /**
    * 是否含有非字母的特殊符号
    *
    * @return
    */
  def hasSpecialLetter(): Boolean = {
    var result = false
    if (index < src.length) {
      val char = src(index)
      char match {
        case isSpecial if
        char == '(' || char == ')' || char == '{' || char == '}' ||
          char == '>' || char == '<' || char == '=' || char == ',' ||
          char == '+' || char == '*' || char == '-' || char == '/' ||
          char == ';'
        => result = true
        case _ =>
      }
    }
    result
  }

  /**
    * 回退多少个位置
    *
    * @param length
    */
  def rollback(length: Int): Unit = {
    index = index - length
    lineIndex = lineIndex - length
  }

  /**
    * 向前推进多少个位置
    *
    * @param length
    */
  def forward(length: Int): Unit = {
    index = index + length
    lineIndex = lineIndex + length
  }

  def next(length: Int): Unit = {
    for (i <- 0 until length)
      nextToken()
  }

  /**
    * 下个特殊符号token
    *
    * @return
    */
  def nextSpecialLetter(): TinaToken = if (hasSpecialLetter()) {
    val name = src(index).toString
    val tinaMachine = TinaStateMachine(this)
    forward(1)
    if (name == TinaToken.tokenNames(TinaToken.LEFT_PARENT)) {
      TinaToken(name, TinaToken.LEFT_PARENT, line, lineIndex)
    } else if (name == TinaToken.tokenNames(TinaToken.RIGHT_PARENT)) {
      TinaToken(name, TinaToken.RIGHT_PARENT, line, lineIndex)
    } else if (name == TinaToken.tokenNames(TinaToken.LEFT_BRACE)) {
      TinaToken(name, TinaToken.LEFT_BRACE, line, lineIndex)
    } else if (name == TinaToken.tokenNames(TinaToken.RIGHT_BRACE)) {
      TinaToken(name, TinaToken.RIGHT_BRACE, line, lineIndex)
    } else if (name == TinaToken.tokenNames(TinaToken.COMMA)) {
      nextLetters(TinaToken(name, TinaToken.COMMA, line, lineIndex))
    }else if (name == TinaToken.tokenNames(TinaToken.SEMICOLON)) {
      TinaToken(name, TinaToken.SEMICOLON, line, lineIndex)
    } else {
      rollback(1)
      val state = tinaMachine.nextTinaState()
      if (Objects.nonNull(state)) {
        if (state.state == "=")
          TinaToken(name, TinaToken.EQUAL, line, lineIndex)
        else if (state.state == "+")
          TinaToken(name, TinaToken.ADD, line, lineIndex)
        else if (state.state == "*")
          TinaToken(name, TinaToken.MUL, line, lineIndex)
        else if (state.state == "/")
          TinaToken(name, TinaToken.DIV, line, lineIndex)
        else if (state.state == "-")
          TinaToken(name, TinaToken.SUB, line, lineIndex)
        else
          throw MisMatchTokenException("expecting special letter but not found")
      }
      else
        throw MisMatchTokenException("expecting special letter but not found")
    }

  }
  else throw MisMatchTokenException("expecting special letter but not found")

  /**
    * 下个token
    *
    * @return
    */
  def nextToken(): TinaToken = {
    var token: TinaToken = null
    if (index < src.length) {
      val char = src(index)
      char match {
        case skipToken if char == '\t' || char == ' ' || char == '\r' || (char == '/' && (index + 1) < src.length && src(index + 1) == '*') =>
          skip()
          val flag = isInfer
          isInfer = false
          token = nextToken()
          isInfer = flag

        case nextLine if char == '\n' =>
          lineIndex = 0
          line = line + 1
          index = index + 1
          val flag = isInfer
          isInfer = false
          token = nextToken()
          isInfer = flag

        case isNum if hasNumber() =>
          token = nextNumber(TinaToken(0, TinaToken.INT, line, lineIndex - 1), 0)

        case isLetters if hasLetter() =>
          token = nextLetters(TinaToken("", TinaToken.VAR, line, lineIndex))

        case isSpecialLetters if hasSpecialLetter() =>
          token = nextSpecialLetter()

        case _ =>
          throw MisMatchTokenException(char + " can not match")

      }
    }


    token
  }

  /**
    * 向前同步N个token，有利于判断该Token是哪个Token
    *
    * @param size 同步的个数
    */
  def syn(size: Int): Unit = {
    isInfer = true
    if (isInfer)
      inferIndexs = (index, lineIndex) +: inferIndexs
    for (i <- 0 until size) {
      buffer = buffer :+ nextToken()
    }
    if (isInfer) {
      index = inferIndexs.head._1
      lineIndex = inferIndexs.head._2
      inferIndexs = inferIndexs.slice(1, inferIndexs.length)
    }
    isInfer = false
  }

  /**
    * 重置缓冲区
    */
  def reset(): Unit = {
    buffer = List[TinaToken]()
  }
}

/**
  * 状态描述
  *
  * @param char      状态符
  * @param nextPos   下个状态开始位置
  * @param nextCount 下个状态的状态大小
  * @param pos       当前状态的位置
  * @param state     状态描述
  */
case class TinaState(char: Char, nextPos: Int, nextCount: Int, pos: Int, state: String) {

}

object TinaState {
  val state1: List[TinaState] = List[TinaState](
    TinaState('=', 0, 1, 0, "="),
    TinaState('>', 1, 2, 0, ">"),
    TinaState('+', 3, 0, 0, "+"),
    TinaState('-', 5, 0, 0, "-"),
    TinaState('/', 0, 0, 0, "/"),
    TinaState('*', 0, 0, 0, "*")
  )

  val state2: List[TinaState] = List[TinaState](
    TinaState('=', 0, 0, state1.length + 0, "=="),
    TinaState('>', 0, 1, state1.length + 1, ">>"),
    TinaState('=', 0, 0, state1.length + 2, ">="),
    TinaState('=', 0, 0, state1.length + 3, "+="),
    TinaState('+', 0, 0, state1.length + 4, "++"),
    TinaState('=', 0, 0, state1.length + 5, "-="),
    TinaState('-', 0, 0, state1.length + 6, "--"),
    TinaState('=', 0, 0, state1.length + 7, "/="),
    TinaState('=', 0, 0, state1.length + 8, "*=")
  )
  val state3: List[TinaState] = List[TinaState](
    TinaState('=', 0, 0, 0, ">>=")
  )
}

case class TinaStateMachine(lexer: TinaLexer) {

  def nextTinaState(): TinaState = {
    def matchTinaState(group: Int, pos: Int, count: Int, state: TinaState): TinaState = {

      val groupState = group match {
        case 1 => TinaState.state1
        case 2 => TinaState.state2
        case 3 => TinaState.state3
      }
      for (i <- pos until pos + count if i < groupState.length && lexer.index < lexer.src.length) {
        if (lexer.src(lexer.index) == groupState(i).char) {
          lexer.forward(1)
          if (groupState(i).nextCount > 0) {
            return matchTinaState(group + 1, groupState(i).nextPos, groupState(i).nextCount, groupState(i))
          } else {
            return groupState(i)
          }
        }

      }
      state
    }

    matchTinaState(1, 0, TinaState.state1.length, null)
  }
}

//case class


case class TinaType(tinaType: Int) {
  override def toString: String = TinaType.typeNames(tinaType)
}

object TinaType {
  val IDENTIFIER = 0
  val LITERAL = 1
  val BINARY_EXPRESSION = 2
  val CALLEE = 3
  val TINA_OBJECT = 4
  val TINA_PROPERTY = 5
  val TINA_ARGUMENT = 6
  val MEMBER_EXPRESSION = 7
  val CALL_EXPRESSION = 8
  val DECLARATION = 9
  val VARIABLE_DECLARATOR = 10
  val EXPRESSION_STATEMENT = 11
  val BLOCK_STATEMENT = 12
  val ASSIGNMENT_EXPRESSION = 13
  val RETURN_STATEMENT = 14
  val FUNCTION_DECLARATION = 15

  val typeNames = List[String](
    "IDENTIFIER",
    "LITERAL",
    "BINARY_EXPRESSION",
    "CALLEE",
    "TINA_OBJECT",
    "TINA_PROPERTY",
    "TINA_ARGUMENT",
    "MEMBER_EXPRESSION",
    "CALL_EXPRESSION",
    "DECLARATION",
    "VARIABLE_DECLARATION",
    "EXPRESSION_STATEMENT",
    "BLOCK_STATEMENT",
    "ASSIGNMENT_EXPRESSION",
    "RETURN_STATEMENT",
    "FUNCTION_DECLARATION"
  )
}

case class TinaScope(tinaScope: Int) {
  override def toString: String = TinaScope.scopeNames(tinaScope)
}

object TinaScope {
  val scopeNames = List[String]()
}

/**
  * 标识符
  *
  * @param tType    类型
  * @param tinaType 类型
  * @param name     名字
  */
case class Identifier(tType: Int, tinaType: String, name: String)

/**
  * 字面量
  *
  * @param tType    类型
  * @param tinaType 类型
  * @param value    值
  * @param raw      原生类型
  */
case class Literal(tType: Int, tinaType: String, value: Any, raw: String)

/**
  * 二分表达式(包括比较，赋值)
  *
  * @param tType    类型
  * @param tinaType 类型
  * @param operator 作用符
  * @param left     左式
  * @param right    右式
  */
case class BinaryExpression(tType: Int, tinaType: String, operator: String, left: Any, right: Any)

/**
  * 普通调用函数表达式的名称
  *
  * @param tinaType
  * @param name
  */
case class Callee(tinaType: String, name: String)

/**
  * 实例化的对象
  *
  * @param tinaType
  * @param name
  */
case class TinaObject(tinaType: String, name: String)

/**
  * 实例化的对象属性
  *
  * @param tinaType
  * @param name
  */
case class TinaProperty(tinaType: String, name: String)

/**
  * 调用成员表达式
  *
  * @param tinaType
  * @param computed
  * @param tinaObject
  * @param tinaProperty
  */
case class MemberExpression(tinaType: String, computed: Boolean, tinaObject: TinaObject, tinaProperty: TinaProperty)

/**
  * 调用表达式,callee表达的是Callee或者MemberExpression
  *
  * @param tinaType
  * @param callee
  * @param arguments
  */
case class CallExpression(tinaType: String, callee: Any, arguments: List[Any])

/**
  * 声明并且包含初始化
  *
  * @param tinaType 类型
  * @param id
  * @param init
  */
case class Declaration(tinaType: String, id: Identifier, init: Any)

/**
  * 变量的声明，并且包含初始化
  *
  * @param tinaType
  * @param declarations
  */
case class VariableDeclaration(tinaType: String, declarations: List[Declaration])

/**
  * 表达式语句
  *
  * @param tinaType
  * @param expression
  */
case class ExpressionStatement(tinaType: String, expression: Any)

/**
  * 块语句
  *
  * @param tinaType
  * @param body
  */
case class BlockStatement(tinaType: Int, desc: String, body: List[Any])

/**
  * 函数返回语句
  *
  * @param tinaType
  * @param argument
  */
case class ReturnStatement(tinaType: Int, desc: String, argument: Any)

/**
  * 函数声明
  *
  * @param tinaType
  * @param id
  * @param params
  */
case class FunctionDeclaration(tinaType: Int, desc: String, id: Identifier, params: List[Identifier]) {
  var body: BlockStatement = _
}

/**
  * 转换异常
  *
  * @param e
  */
case class TinaParseException(e: String) extends RuntimeException(e)

case class TinaParser(lexer: TinaLexer) {
  // 函数表
  var functionDeclarations: List[FunctionDeclaration] = List[FunctionDeclaration]()
  // 全局变量表
  var globalVariable: List[VariableDeclaration] = List[VariableDeclaration]()
  // 是否是全局变量
  var isGlobal = true

  /**
    * 变量类型赋值
    *
    * @param left
    * @param right
    */
  def assignWithVar(left: TinaToken, right: TinaToken): Unit = {
    val leftIdentifier = Identifier(TinaType.IDENTIFIER, TinaType.typeNames(TinaType.IDENTIFIER), left.name.asInstanceOf[String])
    val rightIdentifier = Identifier(TinaType.IDENTIFIER, TinaType.typeNames(TinaType.IDENTIFIER), right.name.asInstanceOf[String])
    val declaration = Declaration(TinaType.typeNames(TinaType.DECLARATION), leftIdentifier, rightIdentifier)
    if (isGlobal)
      globalVariable = globalVariable :+ VariableDeclaration(TinaType.typeNames(TinaType.VARIABLE_DECLARATOR), List(declaration))

  }

  /**
    * Int类型赋值
    *
    * @param left
    * @param right
    */
  def assignWithInt(left: TinaToken, right: TinaToken): Unit = {
    val identifier = Identifier(TinaType.IDENTIFIER, TinaType.typeNames(TinaType.IDENTIFIER), left.name.asInstanceOf[String])
    val literal = Literal(TinaType.LITERAL, TinaType.typeNames(TinaType.LITERAL), right.name.asInstanceOf[Int], String.valueOf(right.name))
    val declaration = Declaration(TinaType.typeNames(TinaType.DECLARATION), identifier, literal)
    if (isGlobal)
      globalVariable = globalVariable :+ VariableDeclaration(TinaType.typeNames(TinaType.VARIABLE_DECLARATOR), List(declaration))
  }

  def matchBlockStatement(lexer: TinaLexer): BlockStatement = {
    def matchBody(lexer: TinaLexer, expressionStatements: List[Any]): (List[Any]) = {
      ExpressionStateMachine(lexer).nextExpressions(expressionStatements)
    }

    BlockStatement(TinaType.BLOCK_STATEMENT, TinaType.typeNames(TinaType.BLOCK_STATEMENT), matchBody(lexer, List()))
  }

  /**
    * 函数类型赋值
    *
    * @param funcName
    */
  def assignWithFunction(lexer: TinaLexer, funcName: TinaToken): FunctionDeclaration = {
    def assignWithFunctionParams(lexer: TinaLexer, params: List[Identifier]): List[Identifier] = {
      var result = params
      if (lexer.isNextTokenOf(TinaToken.RIGHT_PARENT)) {
        lexer.nextToken()
        params
      } else {
        val param = lexer.nextToken()
        val identifier = Identifier(TinaType.IDENTIFIER, TinaToken.convertType(param.kind), param.name.asInstanceOf[String])
        result = result :+ identifier
        if (lexer.isNextTokenOf(TinaToken.COMMA))
          lexer.matchToken(TinaToken.COMMA)
        assignWithFunctionParams(lexer, result)
      }
    }

    val identifier = Identifier(TinaType.IDENTIFIER, TinaType.typeNames(TinaType.IDENTIFIER), funcName.name.asInstanceOf[String])
    if (lexer.buffer(1).kind == TinaToken.LEFT_PARENT) {
      lexer.next(2)
      val funcParams = assignWithFunctionParams(lexer, List[Identifier]())
      val funcDecl = FunctionDeclaration(TinaType.FUNCTION_DECLARATION, TinaType.typeNames(TinaType.FUNCTION_DECLARATION), identifier, funcParams)
      lexer.reset()
      lexer.syn(1)
      if (lexer.buffer(0).kind == TinaToken.LEFT_BRACE) {
        val funcBody = matchBlockStatement(lexer)
        funcDecl.body = funcBody
      }
      funcDecl

    } else throw TinaParseException("left parent not found")
  }

  def parse(): Unit = {
    lexer.reset()
    val token = lexer.nextToken()
    if (Objects.nonNull(token)) {
      if (token.kind == TinaToken.LOVE) {
        lexer.syn(2)
        if (lexer.buffer.length == 2) {
          if (lexer.buffer(0).kind == TinaToken.FUNCTION) {
            functionDeclarations = functionDeclarations :+ assignWithFunction(lexer, lexer.buffer(0))
          } else
            throw TinaParseException("function name not found")
        } else
          throw TinaParseException("both function name and left parent are not found")


      } else if (token.kind == TinaToken.VAR) {
        lexer.syn(2)

        if (lexer.buffer(0).kind == TinaToken.EQUAL) {
          if (lexer.buffer(1).kind == TinaToken.VAR) {
            lexer.next(2)
            val result = lexer.buffer(1)
            assignWithVar(token, result)
            parse()
          } else if (lexer.buffer(1).kind == TinaToken.INT) {
            lexer.next(2)
            val result = lexer.buffer(1)
            assignWithInt(token, result)
            parse()
          }
        }
      } else
        throw new TinaParseException("Cannot recognize token based on " + token)
    }

  }

  def run(): Unit = {
    parse()
  }
}

case class ExpressionStateMachine(lexer: TinaLexer) {
  var leftBraces = List[TinaToken]()

  /**
    * 二元表达式，优先级： 括号 > 乘除 > 加减
    *
    * @param lexer 词法解析器
    * @return
    */
  def binaryExp(lexer: TinaLexer): Any = {
    // 运算符栈
    var opstack: List[TinaToken] = List[TinaToken]()
    // 操作数栈
    var astStack: List[Any] = List[Any]()

    /**
      * 根据条件构建表达式
      *
      * @param fn
      */
    def build(fn: () => Boolean): Unit = {
      if (fn()) {
        val opToken = opstack.head
        opstack = opstack.drop(1)
        val right = astStack.head
        astStack = astStack.drop(1)
        val left = astStack.head
        astStack = astStack.drop(1)
        val binary = BinaryExpression(TinaType.BINARY_EXPRESSION, TinaType.typeNames(TinaType.BINARY_EXPRESSION), opToken.name.asInstanceOf[String], left, right)
        astStack = binary +: astStack
        build(fn)
      }
    }

    /**
      * 构建表达式
      *
      * @param token token
      */
    def buildBinary(token: TinaToken): Unit = {
      if (!opstack.forall(_ == null) && priority(opstack.head) >= priority(token)) {
        build(() => !opstack.forall(_ == null))
        buildBinary(token)
      }
    }

    /**
      * 找出token的优先级
      *
      * @param token token
      * @return
      */
    def priority(token: TinaToken): Int = {
      if (token.kind == TinaToken.LEFT_PARENT)
        return 0
      else if (token.kind == TinaToken.ADD || token.kind == TinaToken.SUB)
        return 1
      else if (token.kind == TinaToken.DIV || token.kind == TinaToken.MUL)
        return 2
      else if (token.kind == TinaToken.RIGHT_PARENT)
        return 3
      -1
    }


    def run(): Unit = {
      lexer.reset()
      lexer.syn(1)
      if (!lexer.buffer.forall(_ == null)) {
        val buf = lexer.buffer.head
        if (buf.kind != TinaToken.SEMICOLON) {
          if (buf.kind == TinaToken.LEFT_PARENT) {
            val token = lexer.nextToken()
            opstack = token +: opstack
          } else if (buf.kind == TinaToken.VAR) {
            val token = lexer.nextToken()
            val identifier = Identifier(TinaType.IDENTIFIER, TinaType.typeNames(TinaType.IDENTIFIER), token.name.asInstanceOf[String])
            astStack = identifier +: astStack
          } else if (buf.kind == TinaToken.INT) {
            val token = lexer.nextToken()
            val literal = Literal(TinaType.LITERAL, TinaType.typeNames(TinaType.LITERAL), token.name,
              token.name.toString)
            astStack = literal +: astStack
          } else if (buf.kind == TinaToken.RIGHT_PARENT) {
            lexer.nextToken()
            build(() => !opstack.forall(_ == null) && opstack.head.kind != TinaToken.LEFT_PARENT)
            opstack = opstack.drop(1)
          } else if (buf.kind == TinaToken.ADD || buf.kind == TinaToken.SUB || buf.kind == TinaToken.MUL || buf.kind == TinaToken.DIV) {
            if (opstack.isEmpty) {
              val token = lexer.nextToken()
              opstack = token +: opstack
            } else if (!opstack.forall(_ == null)) {
              val nowToken = lexer.nextToken()
              val stackTop = opstack.head
              if (priority(nowToken) > priority(stackTop)) {
                opstack = nowToken +: opstack
              } else {
                buildBinary(nowToken)
                opstack = nowToken +: opstack
              }
            }
          }
          run()
        }else
          lexer.nextToken()

      }
    }

    run()
    build(() => !opstack.forall(_ == null))

    astStack.head
  }

  /**
    * 返回语句
    *
    * @param lexer
    * @return
    */
  def returnExp(lexer: TinaLexer): Any = {
    val token = lexer.nextToken()

    ReturnStatement(TinaType.RETURN_STATEMENT, TinaType.typeNames(TinaType.RETURN_STATEMENT), binaryExp(lexer))
  }

  /**
    * if表达式的初始化
    *
    * @param lexer
    * @return
    */
  def ifInit(lexer: TinaLexer): Any = {
    val leftParent = lexer.nextToken()
    if (leftParent.kind == TinaToken.LEFT_PARENT) {

    }

  }

  /**
    * 函数调用
    *
    * @param callee
    * @param lexer
    * @return
    */
  def callInit(callee: Identifier, lexer: TinaLexer): Any = {
    lexer.reset()
    lexer.syn(1)

    def initArgus(lexer: TinaLexer, arguments: List[Any], comma: List[TinaToken]): List[Any] = {
      lexer.reset()
      lexer.syn(1)
      if (lexer.buffer(0).kind == TinaToken.COMMA) {
        val token = lexer.nextToken()
        return initArgus(lexer, arguments, comma :+ token)
      } else if (lexer.buffer(0).kind == TinaToken.VAR) {
        val token = lexer.nextToken()
        val identifier = Identifier(TinaType.IDENTIFIER, TinaType.typeNames(TinaType.IDENTIFIER), token.name.asInstanceOf[String])
        return initArgus(lexer, arguments :+ identifier, comma.drop(1))
      } else if (lexer.buffer(0).kind == TinaToken.INT) {
        val token = lexer.nextToken()
        val literal = Literal(TinaType.LITERAL, TinaType.typeNames(TinaType.LITERAL), token.name.asInstanceOf[Int], String.valueOf(token.name.asInstanceOf[Int]))
        return initArgus(lexer, arguments :+ literal, comma.drop(1))
      } else if (lexer.buffer(0).kind == TinaToken.RIGHT_PARENT) {
        val token = lexer.nextToken()
        if (comma.isEmpty)
          return arguments
        else
          throw MisMatchTokenException("unexpected token " + token.name.asInstanceOf[String])
      }
      return arguments
    }


    if (lexer.buffer(0).kind == TinaToken.LEFT_PARENT) {
      lexer.nextToken()
      val calle = Callee(TinaType.typeNames(TinaType.IDENTIFIER), callee.name)
      val arguments: List[Any] = initArgus(lexer, List[Any](), List[TinaToken]())
      val expression = CallExpression(TinaType.typeNames(TinaType.CALL_EXPRESSION), calle, arguments)
      val expressionStatement = ExpressionStatement(TinaType.typeNames(TinaType.CALL_EXPRESSION), expression)
      return expressionStatement
    }
    return callee
  }

  /**
    * 变量初始化
    *
    * @param left
    * @param lexer
    * @return
    */
  def variableInit(left: Identifier, lexer: TinaLexer): Any = {
    lexer.reset()
    lexer.syn(1)
    var declaration: Declaration = null
    if (lexer.buffer.head.kind == TinaToken.EQUAL) {
      lexer.nextToken()
      declaration = Declaration(TinaType.typeNames(TinaType.DECLARATION), left, binaryExp(lexer))
    } else {
      declaration = Declaration(TinaType.typeNames(TinaType.DECLARATION), left, null)
    }
    VariableDeclaration(TinaType.typeNames(TinaType.VARIABLE_DECLARATOR), List(declaration))

  }

  def nextExpressions(expressions: List[Any]): List[Any] = {
    var expressionStatements = expressions
    var token = lexer.nextToken()
    if (Objects.isNull(token)) {
      if (leftBraces.isEmpty)
        expressionStatements
      else
        throw MisMatchTokenException("not enough right brace to compare with left brace")
    } else {
      if (token.kind == TinaToken.LEFT_BRACE) {
        leftBraces = token +: leftBraces
        return nextExpressions(expressionStatements)
      } else if (token.kind == TinaToken.RIGHT_BRACE) {
        if (leftBraces.nonEmpty) {
          leftBraces = leftBraces.drop(1)
          return nextExpressions(expressionStatements)
        }
        else
          throw new RuntimeException("not enough left brace to compare with right brace")
      } else {
        if (token.kind == TinaToken.VAR) {
          val leftIdentifier = Identifier(TinaType.IDENTIFIER, TinaType.typeNames(TinaType.IDENTIFIER), token.name.asInstanceOf[String])
          expressionStatements = expressionStatements :+ variableInit(leftIdentifier, lexer)
          return nextExpressions(expressionStatements)
        } else if (token.kind == TinaToken.FUNCTION) {
          val funcIdentifier = Identifier(TinaType.IDENTIFIER, TinaType.typeNames(TinaType.IDENTIFIER), token.name.asInstanceOf[String])
          expressionStatements = expressionStatements :+ callInit(funcIdentifier, lexer)
          return nextExpressions(expressionStatements)
        }
        else if (token.kind == TinaToken.RETURN) {
          expressionStatements = expressionStatements :+ returnExp(lexer)
          return nextExpressions(expressionStatements)
        }
      }
      expressionStatements
    }
  }
}
