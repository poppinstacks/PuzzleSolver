import scala.io.Source

/*
    Description: Ad hoc scanner for the calculator language in PLP, 4th Ed
    Author: J. Femister
    Date: September 2017
    Version: 2 - Adapted to be used with Recursive Descent Parser
*/

// List of tokens to be recognized
abstract class Symbol
abstract class Token
case object Lparen extends Token
case object Rparen extends Token
case object Plus extends Token
case object Minus extends Token
case object Times extends Token
case object Div extends Token
case object Assign extends Token
case object Read extends Token // keyword
case object Write extends Token // keyword
case class Id(i:String) extends Token
case class Number(n:String) extends Token

abstract class Action
case object Predict extends Action
case object Error extends Action

case object Skip extends Token
case class ScannerError(mess:String) extends Token
case object Eof extends Token


class Scanner(path:String) {
  // Special chars
  val newline = '\n'
  val EOF = '$'

  // Character categories/sets
  val whitespace = Set(' ', '\t', '\n')
  val digits = ('0' to '9').toSet
  val letters = ('A' to 'Z').toSet ++ ('a' to 'z').toSet
  val punctuation = Map('(' -> Lparen, ')' -> Rparen, '+' -> Plus,
    '-' -> Minus, '*' -> Times)
  val keywords = Map("read" -> Read, "write" -> Write)

  // Read in entire file into a single string
  val source = io.Source.fromFile(path).getLines.toList.mkString("\n")

  // Method to return a single char
  var i = -1
  def nextchar = { i += 1; if (i < source.length) source(i) else EOF }

  // Prime the pump
  var cur_char = nextchar

  //Type creates the types for terminal stuff
  type terminal = Int
  type non_terminal = Int
  type symbol = Int
  type production = Int
  type State = Int

  //case that implements ScanTabCell from TableDrivenScanner
  case class ParseTabCell(var action:Action, var new_state:State)
  
  //Arrays for production things and the parse table
  var production_table = new Array[String](11)
  var parse_table = Array.ofDim[Int](11,14)

  //reads in the table
  var parseTable = io.Source.fromFile("src/main/calculatorparsetable.txt").getLines.map(line=>line.split("\\s+").toList).toList

  //takes the parse table and populates the parse_table with it without the nonterminals
  for(row <- 0 until parseTable.length){
    for(column <- 1 until parseTable(0).head.toInt){
      parse_table(row)(column) = parseTable(row)(column).toInt
      if(parseTable(row)(column).toInt > 0){
        ParseTabCell(Predict, parseTable(row)(column).toInt)
      } else{
        ParseTabCell(Error, parseTable(row)(column).toInt)
      }
    }
  }

  //reads in the calculator productions file
  var productionTable = io.Source.fromFile("src/main/calculatorproductions.txt").getLines.map(line=>line.split("\\s+").toList).toList
  var symbolprod_tab = List[symbol]

  //"parses" the production table so that it means something
  for(row <- 0 until productionTable.length){
    for(column <- productionTable(0).length to 1){
      production_table(row) = productionTable(row)(column)
    }
  }

  //creates the parse_stack
  var sizeOfStack:Int = 10
  var parse_stack = new Array[String](sizeOfStack)

  //doubles the size of the stack if the stack runs out of room
  def increaseStack(parse_stack:Array[String]): Unit= {
    var oldSize = parse_stack.length
    var newSize= oldSize*2
    var newparse_stack = new Array[String](newSize)
    for(i <- parse_stack.length){
      parse_stack(i) = newparse_stack(i)
    }
  }

  //reads in the average.txt and divides it
  var input = io.Source.fromFile("src/main/average.txt").getLines().map(line=>line.split("\\s+").toList).toList

  // Main method (called by the parser) to get the next token
  def nexttoken:Token = {

    // Auxiliary method to recognize a token, including the skip
    // "pseudo" token and code

    //starts with the program symbol in the stack
    var start_symbol = production_table(0)
    parse_stack(0) = start_symbol
    print(parse_stack)

    //reads the input and parses using the LL parsing method
    while(nexttoken != Eof) {
      var expected_token: String = parse_stack(parse_stack.length - 1)
      for(row<-production_table.length){
        expected_token match{
          case program => expected_token match{
            case Id.toString => println(expected_token)
            case Read.toString => println(expected_token)
            case Write.toString => println(expected_token)
          }
          case stmt_list => expected_token match {
            case Id.toString => println(expected_token)
            case Read.toString => println(expected_token)
            case Write.toString => println(expected_token)
            case $$ => println("$$")
          }
            /*
          case stmt => expected_token match {

          }
          */
        }
      }
    }


    def gettoken = {

      // Utility method for accumulating a string of digits
      def collectDigits = {
        var digitstr = cur_char.toString
        cur_char = nextchar
        while (digits contains cur_char) {
          digitstr += cur_char
          cur_char = nextchar
        }
        digitstr
      }

      // skip whitespace
      while (whitespace contains cur_char) {
        cur_char = nextchar
      }

      // Recognize a token, based on it's first character
      val token = cur_char match {
        // End of file token
        case EOF => Eof

        // Single char punctuation tokens
        /* guard condition       */
        case ch:Char if punctuation contains ch =>
        { cur_char = nextchar; punctuation(ch) }

        // 2 character assignment token
        case ':' => {
          cur_char = nextchar
          if (cur_char == '=') {
            cur_char = nextchar
            Assign
          } else {
            cur_char = nextchar
            ScannerError("Expected = after :")
          }
        }

        // Comments or the div token
        case '/' => {
          cur_char = nextchar
          if (cur_char == '/') {
            cur_char = nextchar
            while (cur_char != newline) cur_char = nextchar
            cur_char = nextchar
            Skip
          } else if (cur_char == '*') {
            cur_char = nextchar
            var done = false
            while (!done) {
              while (cur_char != '*') cur_char = nextchar
              cur_char = nextchar
              if (cur_char == '/') done = true
            }
            cur_char = nextchar
            Skip
          } else
            Div
        }

        // Number starting with a decimal point
        case '.' => {
          cur_char = nextchar
          if (digits contains cur_char) {
            var numberstr = "." + collectDigits
            Number(numberstr)
          } else
            ScannerError("Expected digit after .")
        }

        // Number starting with a digit
        case d if digits contains d => {
          var numberstr = collectDigits
          if (cur_char == '.') {
            cur_char = nextchar
            numberstr += ("." + collectDigits)
          }
          Number(numberstr)
        }

        // Identifier or keywords
        case kh if letters contains kh => {
          var idstr = "" + kh
          cur_char = nextchar
          while ((letters contains cur_char) || (digits contains cur_char)) {
            idstr += cur_char
            cur_char = nextchar
          }
          if (keywords contains idstr)
            keywords(idstr)
          else
            Id(idstr)
        }

        // Error if none of the other categories fit
        case kh => ScannerError(s"Unrecognized character '$kh'")
      }
      token
    }

    // Return token, throwing away the "skip" token
    var rtoken = gettoken
    while (rtoken == Skip) rtoken = gettoken
    rtoken
  }

}
