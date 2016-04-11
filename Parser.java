/* 		
OBJECT-ORIENTED RECOGNIZER FOR SIMPLE EXPRESSIONS
program -> decls stmts end
decls -> int idlist ';'
idlist -> id [',' idlist ]
stmts -> stmt [ stmts ]
stmt -> assign ';'| cmpd | cond | loop
assign -> id '=' expr
cmpd -> '{' stmts '}'
cond -> if '(' rexp ')' cmpd [ else cmpd ]
loop -> for '(' [assign] ';' [rexp] ';' [assign] ')' stmt

rexp -> expr ('<' | '>' | '==' | '!= ') expr
expr -> term [ ('+' | '-') expr ]
term -> factor [ ('*' | '/') term ]
factor -> int_lit | id | '(' expr ')'    
*/

/*
 * The grammar is parsed as follows
 *    When nonterminal is expected : call corresponding constructor
 *    When terminal is expected    : call Lexer.lex() to tokenize
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class Parser {
	public static boolean bIsKeyWordParsedAlready = false;
	public static boolean bIsAssignedCallInForLoop = false;
	public static String sAssignedStackInForLoop = "";
	public static void main(String[] args){
		System.out.println("Enter an expression, end with semi-colon!");
		System.out.println("The grammar must terminate with keyword 'end'");
		Code.generateOperatorMap();
		new Program();
	}
}

class Program { //program -> decls stmts end
	Decls decls;
	Stmts stmts;
	public Program()
	{
		Lexer.lex();
		decls = new Decls();
		stmts = new Stmts();
		if ( Lexer.nextToken == Token.KEY_END) {
			Code.setPrintStack(Token.KEY_END);
			Code.addByteCodePosForPrintStack();
			System.out.println("******Generated Bytecode*******");
			for ( int i = 0; i< Code.printStack.size(); i++ ){
				System.out.println(Code.printStack.get(i));
			}
			System.out.println("------------------------");
		}
	}
}

class Decls {//decls -> int idlist ';'
	Idlist idList;
	public Decls()
	{
		if ( Lexer.nextToken == Token.KEY_INT) { 
			Lexer.lex();
			idList = new Idlist();
			}
		}
}

class Idlist { //idlist -> id [',' idlist ] 
	char op;
	Idlist idlist;
	public Idlist() {
		if ( Lexer.nextToken == Token.ID) {  // get variable name
			op = Lexer.ident;
			Code.setVariableMap(Character.toString(op),-1);
			Code.variablePtr++;
			Lexer.lex();
			if ( Lexer.nextToken == Token.COMMA) {
				Lexer.lex();
				idlist = new Idlist();
			}
		}
	}
	
}

/*
 * Stmts recursively calls constructor Stmt until we find the token 'end' has arrived which means that program has finished
 * Second condition is when we see a right brace '}' , this is due to grammar rule "cmpd -> '{' stmts '}'"
 */
class Stmts {//stmts -> stmt [ stmts ]
	Stmts stmts;
	Stmt stmt;
	public Stmts()
	{
		stmt = new Stmt();
		if ( Lexer.nextToken != Token.KEY_END && Lexer.nextToken != Token.RIGHT_BRACE) {
			stmts = new Stmts();
		}
	}
}

class Stmt {// stmt -> assign ';' | cmpd | cond | loop
	Assign assign;
	Cmpnd compound;
	Cond cond;
	Loop loop;
	public Stmt() {
		/*
		 * During first stmt, the token is found by the Lexer.lex()
		 * In subsequent calls, we already know the token beforehand for the next statement, hence the condition so as to NOT skip the token 
		 */
		if ((Lexer.nextToken != Token.RIGHT_BRACE) && ( Lexer.nextToken != Token.ID ) && 
			( Lexer.nextToken != Token.LEFT_BRACE) && ( Lexer.nextToken != Token.KEY_IF) 
			&& ( Lexer.nextToken != Token.KEY_FOR ))
			Lexer.lex();
		if ( Lexer.nextToken == Token.ID) { 
			assign = new Assign();
			Lexer.lex(); // to do semicolon
		}
		else if ( Lexer.nextToken == Token.LEFT_BRACE ) {
			compound = new Cmpnd();
		}
		else if ( Lexer.nextToken == Token.KEY_IF ) {
			cond = new Cond();
		}
		else if ( Lexer.nextToken == Token.KEY_FOR ) {
			loop = new Loop();
		}
	}
}

class Assign {//assign -> id '=' expr
	Expr e;
	char op;
	public Assign() {
		if ( Lexer.nextToken == Token.ID) {
			op = Lexer.ident; // this is some variable, if the next statement is equals, we have to push onto stack
			Lexer.lex();//skip the assignment variable
			if ( Lexer.nextToken == Token.ASSIGN_OP) {
				Lexer.lex();//skipping '=' 
				e = new Expr();
				Code.setPrintStack(Character.toString(op), Token.ASSIGN_OP);
			}			
		}
	}
}

class Cmpnd {//cmpd -> '{' stmts '}'
	Stmts stmts;
	public Cmpnd()
	{
		if( Lexer.nextToken == Token.LEFT_BRACE ) {
			Lexer.lex();//moving ahead of left brace
			stmts = new Stmts();
			Lexer.lex(); // ignore right brace
		}
	}	
}

/*
 * Here the final pointer positions for if and else blocks can only be known AFTER the whole statement has been tokenized
 * Hence, the if & else statements in printStack are updated after the tokenizing is done with appropiate byte positions
 */
class Cond { //cond -> if '(' rexp ')' cmpd [ else cmpd ]
	Rexp r;
	Stmt s1,s2;
	Cmpnd compound;
	int byteCodePositionIfBlock;
	public Cond() {
		if ( Lexer.nextToken == Token.KEY_IF) {
			Lexer.lex();
			if(Lexer.nextToken == Token.LEFT_PAREN) { 
				Lexer.lex(); // skip the '(' token
				r=new Rexp(); 
			}
			Lexer.lex(); //skip right ')'
			
			s1 =new Stmt();
		}
		byteCodePositionIfBlock = Code.byteCodePtr;
		if(Lexer.nextToken == Token.KEY_ELSE) {
			Lexer.lex(); // skip the '{' token
			Code.setPrintStack(Token.KEY_ELSE); // push the operator , not the byte position
			byteCodePositionIfBlock = Code.byteCodePtr;
			s2 = new Stmt();
			
			Code.updatePrintStack(Token.KEY_ELSE , Code.byteCodePtr); //'else' pointer position
		}
		Code.updatePrintStack(Token.KEY_IF , byteCodePositionIfBlock); // 'if' pointer position
		
	}
}

class Rexp { //expr ('<' | '>' | '==' | '!= ') expr
	Expr e1,e2;
	int operation;
	public Rexp(){
		e1=new Expr();
		operation = Lexer.nextToken;
		switch(Lexer.nextToken){
		case Token.LESSER_OP :
			Lexer.lex();//moving to the next char after '<'
			break;
		case Token.GREATER_OP :
			Lexer.lex(); //moving to the next char after '>'			
			break;
		case Token.EQ_OP :
			Lexer.lex();//moving to the next char after '=='
			break;
		case Token.NOT_EQ:
			Lexer.lex();//moving to the next char after '!='
			break;
		default:
			break;
		}
		e2=new Expr();
		Code.setPrintStack(operation); // push the operator , not the byte position
	}
}

class Loop{	//loop -> for '(' [assign] ';' [rexp] ';' [assign] ')' stmt	
	Assign a1,a2;		
	Rexp r;		
	Stmt s;	
	ArrayList<String> assignStatements = new ArrayList<String>();
	int blockResetByPos = 0;
	int byteCodePosForStartOfAssignBlock, byteCodePosForStartOfStmtBlock, byteCodePosForEndOfStmtBlock , byteCodePositionForStartOfLoop;
	public Loop(){	
		Lexer.lex();//skipping 'for' token
		if(Lexer.nextToken==Token.LEFT_PAREN){		
			Lexer.lex();//skipping (		
			if(Lexer.nextToken!=Token.SEMICOLON)		
				a1=new Assign();
		byteCodePositionForStartOfLoop = Code.byteCodePtr;
			Lexer.lex();//skipping the first ;	
			if(Lexer.nextToken!=Token.SEMICOLON)		
				r=new Rexp();
			Lexer.lex();//skipping the second ;	
			byteCodePosForStartOfAssignBlock = Code.byteCodePtr;
			Parser.bIsAssignedCallInForLoop = true;
			if(Lexer.nextToken!=Token.RIGHT_PAREN)		
				a2=new Assign();		
			Lexer.lex();
			blockResetByPos = Code.byteCodePtr - byteCodePosForStartOfAssignBlock;
			Code.byteCodePtr = byteCodePosForStartOfAssignBlock;
			Parser.bIsAssignedCallInForLoop = false;
			if (!Parser.sAssignedStackInForLoop.trim().equals(""))
				assignStatements.addAll(Arrays.asList(Parser.sAssignedStackInForLoop.split("\n")));

			Collections.reverse(assignStatements);
			Parser.sAssignedStackInForLoop = "";
			s=new Stmt();		
			for (int index = assignStatements.size()-1;index >=0; index--)
				Code.printStack.add(assignStatements.get(index));

			Parser.sAssignedStackInForLoop = "";
			Code.setPrintStack(Token.KEY_GOTO);
			Code.updatePrintStack(Token.KEY_GOTO, byteCodePositionForStartOfLoop);
			Code.updatePrintStack(Token.KEY_IF, Code.byteCodePtr + blockResetByPos); // KEY_IF since we are doing relational expression in 'for' loop
			Code.byteCodePtr = Code.byteCodePtr + blockResetByPos;

		}		
	}		
}

// For "expr -> term [ ('+' | '-') expr ]"
class Expr {
	int operation;
	Term t;
	Expr e;
	Expr() {
		
		t = new Term();
		//if ( Lexer.nextToken == Token.SEMICOLON)
			//Lexer.lex();
		//Parser.printMessage("In Expr");
		if  ( ( Lexer.nextToken == Token.ADD_OP) || ( Lexer.nextToken == Token.SUB_OP) ) {
			operation = Lexer.nextToken;
			Lexer.lex();
			e = new Expr();
			Code.setPrintStack(operation);
			//something with 'op'
		}
	}
}


// for 
class Term    { // term -> factor (* | /) term | factor
	//term -> factor [ ('*' | '/') term ] --> square brackets indicate expansion is optional
	Factor f;
	Term t;
	char op;
	int operation;
	public Term() {
		f = new Factor();
		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
			op = Lexer.ident; // store this for "iload" statement
			operation = Lexer.nextToken;
			// do something with op here
			Lexer.lex();
			t = new Term();
			//System.out.println("op :" + Character.toString(op) + " operator :" + String.valueOf(Lexer.nextToken));
			//Code.setPrintStack(Character.toString(op),Lexer.nextToken,false);
			Code.setPrintStack(operation); // imul / idiv 
			//Code.gen(Code.opcode(op));
		}
	}
}

class Factor { //factor -> int_lit | id | '(' expr ')'   
	//ID id;
	Expr ex;
	char op;
	int i;
	public Factor()
	{
		switch(Lexer.nextToken){
			case Token.INT_LIT :
				i = Lexer.intValue;
				Code.setVariableMap(Character.toString(  Lexer.ident),i);
				Code.setPrintStack( Character.toString( Lexer.ident), Lexer.nextToken); // istore
				Lexer.lex();
				//Parser.printMessage("after Factor");
				
				break;
			case Token.LEFT_PAREN :
				Lexer.lex();
				//Parser.printMessage("In class Factor");
				ex = new Expr();
				Lexer.lex() ; // skip over RIGHT_PAREN ')', WHY , TODO?
				break;	
			case Token.ID :
				//new ID();
				// Here, ID means that the operation requires a previously stored value to be loaded back, hence iload
				op = Lexer.ident;
				//System.out.println();
				Code.setPrintStack(Character.toString(op), Lexer.nextToken);
				Lexer.lex();
				//Lexer.lex();
			break;	
		}
	}
}

class Code {
	static String[] code = new String[500]; //100 characters is very small for a mid sized program
	static int codeptr = 0;
	static int variablePtr = 1;
	static int CodePtrForQueue = 0;
	static int byteCodePtr = 0; // keeps track of byte in stack e.g. "1: istore_5", this variable will store 1
	static public Queue<String> variableQueue = new LinkedList<String>();
	static public Queue<String> constQueue    = new LinkedList<String>();
	
	static public Map<String, Integer>variableMap = new HashMap<String, Integer>();
	static public Map<Integer, String>operatorMap = new HashMap<Integer, String>();
	static public ArrayList<String> printStack = new ArrayList<String>();
	//*********
	public static void generateOperatorMap() {
		operatorMap.put(Token.KEY_END,"return");
		operatorMap.put(Token.ADD_OP , "iadd");
		operatorMap.put(Token.DIV_OP , "idiv");
		operatorMap.put(Token.MULT_OP, "imul");
		operatorMap.put(Token.SUB_OP , "isub");
		operatorMap.put(Token.ID     , "iload");
		operatorMap.put(Token.GREATER_OP, "if_icmple");
		operatorMap.put(Token.LESSER_OP, "if_icmpge");
		operatorMap.put(Token.EQ_OP, "if_icmpne");
		operatorMap.put(Token.KEY_ELSE, "goto");
		operatorMap.put(Token.KEY_GOTO, "goto");
		operatorMap.put(Token.KEY_IF, "if_icmp");
		operatorMap.put(Token.NOT_EQ,"if_icmpeq");
	}

	public static void genVariables(char ch) {
		//variableQueue.add(Character.toString(ch));
		//variablePtr++;
	}

	public static void genConst(int i) {
		//constQueue.add(String.valueOf(i));
		//CodePtrForQueue ++;
	}
	
	public static int searchInPrintStack(String substringToSearch) { //TODO : redundant remove this
		int index = -1;
		//System.out.println("substr  = " + substringToSearch);
		for ( index = 0 ; index < printStack.size() ;index ++) {
			try {
				if ( printStack.get(index).contains(substringToSearch))
					break;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//System.out.println("Exception at:" + String.valueOf(index));
				//System.out.println("printStack :" + printStack.toString());
			}
		}
		return index;
	}
	/***********************************************************************
	 * 
	 * @param variableName
	 * @param variableValue
	 */
	public static void setVariableMap(String variableName, int variableValue) {
		// it exists, update only variableValue
		int index;
		boolean indexFound = false;
		for ( index = 1; index <= variableMap.size() ; index ++) {
			indexFound = variableMap.containsKey(variableName + "_" + String.valueOf(index));
			if (indexFound) {
				break;
			}			
		}
		if (!indexFound) {
			index = variablePtr;
		}
		variableMap.put(variableName + "_" + String.valueOf(index) ,variableValue);
	}
	
	/*************************************************************
	 * The printStack is rearranged to add the byte code in beginning of each statement
	 * ***********************************************************
	 * */	
	public static void addByteCodePosForPrintStack() {
		int index, byteCodePos, posn;
		String token;
		byteCodePos = 0;
		for  ( index = 0 ; index < printStack.size(); index ++) {
			token = printStack.get(index);
			//remove the unwanted bytecode here
			posn = token.indexOf(":");
			if (posn > 0) 
				token = token.replace(token.substring(0, posn +1), "").trim();
			printStack.set(index, String.valueOf(byteCodePos) + ": " + token);
			
			if (( token.contains("bipush ")) || (token.contains("iconst ")) || (token.contains("iload ")) || (token.contains("istore ")) ) {
				byteCodePos += 1;
			}
			else if (( token.contains("sipush")) || ( token.contains("if_icmp"))  || (token.contains("goto"))) {
				byteCodePos += 2;
			}
			byteCodePos++;	
		}
		
	}	
	/**************************************************
	*For 'if' and 'else' blocks, the byte code pointer needs to be updated to appropriate location
	* This needs to be done AFTER the block has been parsed
	* Hence, we look for last occurence of string {"*icmpge*", "*icmple*" } (if block), OR "*goto*" ( else block)
	* We also check if the found string does not have any integer at the end, this ensures that for nested if-else blocks, the byteCodePtr is appended at 
	* proper place
	* The last character in the found line will be updated with byteCodePtr
	* **************************************************
	* */
	public static void updatePrintStack(int operator, int byteCodePosition) {
		int tokenIndex;
		String operatorString;
		operatorString = operatorMap.get(operator);
		for ( tokenIndex = printStack.size()-1; tokenIndex>=0 ; tokenIndex--) {
			if(printStack.get(tokenIndex).contains(operatorString) && (!printStack.get(tokenIndex).matches("^.+?\\d$") )) {
				break;
			}
		}
		//System.out.println("operator :" + operatorString + " tokenIndex : " + String.valueOf(tokenIndex));
		if ( tokenIndex >= 0 ) {
			printStack.set(tokenIndex, printStack.get(tokenIndex) + " " + String.valueOf(byteCodePosition));
		}	
	}
	
	/*****************************************************
	* Call this function only when some arithmetic / relational expression is to be logged
	* When istore is to be logged, call other function
	******************************************************/
	public static void setPrintStack(int operator) { 
		String operatorString = "";		
		operatorString = Code.byteCodePtr + ": " + operatorMap.get(operator);
		//printStack.add(String.valueOf(byteCodePtr) + ": " + operatorString);
		if (!Parser.bIsAssignedCallInForLoop)
			printStack.add(operatorString);
		else
			Parser.sAssignedStackInForLoop = ( Parser.sAssignedStackInForLoop != "")?(Parser.sAssignedStackInForLoop + "\n" + operatorString)
					 :(Parser.sAssignedStackInForLoop = operatorString);

		byteCodePtr++;
		if (( operator == Token.GREATER_OP) || (operator == Token.LESSER_OP) || (operator == Token.KEY_ELSE)
			|| (operator == Token.EQ_OP) || (operator == Token.KEY_GOTO) || (operator == Token.NOT_EQ))
			byteCodePtr +=2;
	}

	/*****************************************************
	* Call this function for all other types of logging 
	******************************************************/
	public static void setPrintStack(String ident, int operator) {
		// first find the position of ident in variableMap
		int index, valueOfIdent = -1;
		String intCodeString , operatorString, stackPrintStatement = "" ;
		for ( index = 1; index <= variableMap.size() ; index ++) {
			if (variableMap.containsKey(ident + "_" + String.valueOf(index))) {
				break;
			}			
		}	
		valueOfIdent = variableMap.get(ident + "_" + String.valueOf(index));
		operatorString = operatorMap.get(operator);
/*		if ((Parser.bIsAssignedCallInForLoop) && (Parser.sAssignedStackInForLoop.equals(""))) {
			System.out.print("Here it is \n");
			Parser.sAssignedStackInForLoop =  Parser.sAssignedStackInForLoop.concat("shuruwat : " + String.valueOf(byteCodePtr));
		}
*/		
		switch(operator) {
			case Token.ID :{
				if ( index > 3) {
					stackPrintStatement = String.valueOf(byteCodePtr) + ": " + operatorString + " " + index; 
					//stackPrintStatement = operatorString + " " + index;
					byteCodePtr++;
				}
				else {
					stackPrintStatement =  String.valueOf(byteCodePtr) + ": " + operatorString + "_" + index;
					//stackPrintStatement =  operatorString + "_" + index;
				}
				byteCodePtr++;
				break;
			}
			case Token.ASSIGN_OP: //istore only
			{

				stackPrintStatement = String.valueOf(byteCodePtr)+ ": " + variableCount(index);
				//stackPrintStatement = variableCount(index);
				byteCodePtr++;
				if ( index  >3) 
					byteCodePtr++;
				break;
			}	
			case Token.INT_LIT : // iconst operations
			{	
				intCodeString = intcode(valueOfIdent);
				stackPrintStatement = String.valueOf(byteCodePtr)+ ": " + intCodeString ;
				//stackPrintStatement = intCodeString ;
				if (intCodeString.indexOf("iconst_" + valueOfIdent) >= 0) {
					byteCodePtr++;
				}
				if  (intCodeString.indexOf("bipush " + valueOfIdent) >= 0) {
					byteCodePtr += 2;
				}
				else if  (intCodeString.indexOf("sipush " + valueOfIdent) >= 0) {
					byteCodePtr += 3;
				}	
				break;
			}
		}
		
		if (!Parser.bIsAssignedCallInForLoop) {
			printStack.add(stackPrintStatement);
		}	
		else {
			//System.out.println("At end of assign :" + String.valueOf(Code.byteCodePtr));
			Parser.sAssignedStackInForLoop = ( !Parser.sAssignedStackInForLoop.equals(""))?(Parser.sAssignedStackInForLoop + "\n" + stackPrintStatement)
											 :(Parser.sAssignedStackInForLoop = stackPrintStatement);
		}	
			
	}

	public static String variableCount(int index) { // this method generates the string required for the corresponding variable
		if (index > 3) { 
			return "istore " + index;
		}			
		else {			
			return "istore_" + index;
		}	
	}

	public static String intcode(int i) {
		if (i > 127) return "sipush " + i;
		if (i > 5) return "bipush " + i;
		return "iconst_" + i;
	}
	
	public static String opcode(char op) {
		switch(op) {
		case '+' : return "iadd";
		case '-':  return "isub";
		case '*':  return "imul";
		case '/':  return "idiv";
		default: return "";
		}
	}

	public static void outputVariables() {
		for (int i=0; i<codeptr; i++)
			System.out.println(code[i]);
	}
}


