/* *** This file is given as part of the programming assignment. *** */

import java.util.LinkedList;
import java.util.Stack;


public class Parser {


    // Here are some macros for first sets
    private static final TK[] STATEMENT_ARR = {TK.TILDE, TK.ID, TK.PRINT, TK.DO, TK.IF, TK.FOR};
    private static final TK[] REF_ID_ARR = {TK.TILDE, TK.ID};
    private static final TK[] ASSIGN_ARR = REF_ID_ARR;
    private static final TK[] ADDOP_ARR = {TK.PLUS, TK.MINUS};
    private static final TK[] MULTOP_ARR = {TK.TIMES, TK.DIVIDE};

    private Stack<LinkedList<String>> symbolTable = new Stack<LinkedList<String>>();

    // tok is global to all these parsing methods;
    // scan just calls the scanner's scan method and saves the result in tok.
    private Token tok; // the current token
    private void scan() {
        tok = scanner.scan();
    }

    private Scan scanner;
    Parser(Scan scanner) {
        this.scanner = scanner;
        scan();
        program();
        if( tok.kind != TK.EOF )
            parse_error("junk after logical end of program");
    }

    private void program() {
        System.out.print("#include <stdio.h>\nmain()");
        block();
    }

    private void block(){
        System.out.print("{\n");
        
        symbolTable.push(new LinkedList<String>());
        declaration_list();
        statement_list();
        symbolTable.pop();
        
        System.out.print("}\n");
    }

    private void declaration_list() {
        // below checks whether tok is in first set of declaration.
        // here, that's easy since there's only one token kind in the set.
        // in other places, though, there might be more.
        // so, you might want to write a general function to handle that.
        while( is(TK.DECLARE) ) {
            declaration();
        }
    }

    private void declaration() {

        mustbe(TK.DECLARE);
        addToSymbolTable();

        while( is(TK.COMMA) ) {
            mustbe(TK.COMMA);
            addToSymbolTable();
        }
    }

    private void statement_list() {
        while( isMany(STATEMENT_ARR) ){
            statement();
        }

    }

    private void statement() {

        if( isMany(ASSIGN_ARR) ){
            assigment();
        }
        else if ( is(TK.PRINT) ){
            print();
        }
        else if ( is(TK.DO) ){
            e_Do();
        }
        else if ( is(TK.IF) ){
            e_If();
        }
        else if ( is(TK.FOR) ){

        	e_For();
        }
        else{
            System.err.println (tok + " error in statement();");
        }

    }

    private void print() {
        // System.err.println (tok + " in print()");
        mustbe(TK.PRINT);
       
        System.out.print("printf(\"%d\\n\", ");
        
        expr();
        
        System.out.print(");\n");
    }

    private void assigment() {
        // System.err.println (tok + " in assignment()");
        ref_id();
        
        mustbe(TK.ASSIGN);
        System.out.print(" = ");

        expr();
        
        System.out.print(";\n");
    }

    private void ref_id() {

        int scopeModifier = 0;
        int detectVarLevel = 0;

        if( is(TK.TILDE)){
            mustbe(TK.TILDE);
            detectVarLevel = 0;

            if( is(TK.NUM)){

                scopeModifier = Integer.parseInt(tok.string);
                detectVarLevel = whichScope(scopeModifier);
                mustbe(TK.NUM);
                if(!isInScope(scopeModifier)){
                    System.err.println("no such variable ~" + scopeModifier
                        + tok.string + " on line " + tok.lineNumber);
                    System.exit(1);
                }

            }
            else{
                if(!isInScope(symbolTable.size() - 1)){
                    System.err.println("no such variable ~"
                        + tok.string + " on line " + tok.lineNumber);
                    System.exit(1);
                }
            }
        }
        else{
            detectVarLevel = whereIsTok(tok.string);
        }
 
        if(!isInSymbolTable(tok)){
            System.err.println(tok.string + " is an undeclared variable on line "
                                + tok.lineNumber);
            System.exit(1);
        }
        else{

        }
        System.out.print("c_" + tok.string + "_" + detectVarLevel );
        mustbe(TK.ID);

    }

    private void expr() {
        term();
        while( isMany(ADDOP_ARR) ){
            if(is(TK.PLUS)){
                mustbe(TK.PLUS);
                System.out.print(" + ");
            }
            else{
                mustbe(TK.MINUS);
                System.out.print(" - ");
            }

            term();
        }
    }

    private void term() {
        factor();
        while( isMany(MULTOP_ARR) ){
            if(is(TK.TIMES)){
                mustbe(TK.TIMES);
                System.out.print(" * ");
            }
            else{
                mustbe(TK.DIVIDE);
                System.out.print(" / ");
            }

            factor();
        }

    }

    private void factor() {
        if( is(TK.LPAREN) ){
            mustbe(TK.LPAREN);
            System.out.print(" ( ");
            expr();
            mustbe(TK.RPAREN);
            System.out.print(" ) ");
        }
        else if( is(TK.NUM) ){
            System.out.print( Integer.parseInt(tok.string));
            mustbe(TK.NUM);
        }
        else if( isMany(REF_ID_ARR) ){
            ref_id();
        }
        else{
            // System.err.println (tok + " error in factor()");
        }

    }


    private void e_Do() {
        // System.err.println (tok + " in e_Do()");
        mustbe(TK.DO);
        System.out.print("while");
        guarded_command();
        mustbe(TK.ENDDO);
    }

    private void e_If() {
        // System.err.println (tok + " in e_If()");
        mustbe(TK.IF);
        System.out.print("if");
        guarded_command();
        while(is(TK.ELSEIF)){
            mustbe(TK.ELSEIF);
            System.out.print("else if");
            guarded_command();
        }

        if(is(TK.ELSE)){
            mustbe(TK.ELSE);
            System.out.print("else");
            block();
        }

        mustbe(TK.ENDIF);


        // System.err.println (tok + " in e_If()");

    }
    
   private void e_For() {
    	mustbe(TK.FOR);
    	
        System.out.print("int iterator" + symbolTable.size() + ";\nfor");
        for_command();
        mustbe(TK.ENDFOR);
    }

    private void for_command() {
    	for_expr();
        block();

    }    
    
    private void for_expr() {
    	System.out.print("(iterator" + symbolTable.size() + " = ");
    	expr();
        System.out.print("; iterator" + symbolTable.size() + " < ");
    	expr();
        System.out.print("; iterator" + symbolTable.size() + "++)");
        mustbe(TK.THEN);
    }

    private void guarded_command() {
        // System.err.println (tok + " in guarded_command()");
        System.out.println("(");
        expr();
        System.out.println(" <= 0)");
        mustbe(TK.THEN);
        block();
    }





    // is current token what we want?
    private boolean is(TK tk) {
        return tk == tok.kind;
    }

    private boolean isMany(TK[] tk){

        for (int i = 0; i < tk.length; i++){
            if( tk[i] == tok.kind){
                return true;
            }
        }

        return false;
    }

    // ensure current token is tk and skip over it.
    private void mustbe(TK tk) {
        if( ! is(tk) ) {
            System.err.println( "mustbe: want " + tk + ", got " +
                                    tok);
            parse_error( "missing token (mustbe)" );
        }
        scan();
    }

    private void parse_error(String msg) {
        System.err.println( "can't parse: line "
                            + tok.lineNumber + " " + msg );
        System.exit(1);
    }


    // Adds tok to the current scope of symbol table
    private void addToSymbolTable(){
        int stackPos = symbolTable.size() - 1;
        String tempTok = tok.string;


        if(!isInLatestScope(tempTok)){
            symbolTable.elementAt(stackPos).add(tempTok);
            // System.out.println(symbolTable);
            System.out.print("int c_" + tok.string + "_" + (symbolTable.size() - 1) + ";\n");
        }
        else{
            System.err.println("redeclaration of variable " + tempTok);
        }

        mustbe(TK.ID);
        return;
    }

    // Checks if top of symbol table contains the variable (tok)
    private boolean isInLatestScope(String theTok){

        int stackPos = symbolTable.size() - 1;
        int exists = symbolTable.elementAt(stackPos).indexOf(theTok);

        if(exists == -1){
            return false;
        }
        else{
            return true;
        }   
    }

    // Checks if whole symbol table contains the variable (tok)
    private boolean isInSymbolTable(Token theTok){

        int check = 0;

        for(int i = 0; i < symbolTable.size(); i++){
            check = symbolTable.elementAt(i).indexOf(theTok.string);
            if(check != -1){
                return true;
            }
        }

        return false;
    }

    private int whichScope(int scopeModifier){

        int currScope = symbolTable.size() - 1;

        if(currScope >= scopeModifier){
            return (currScope - scopeModifier);
        }
        else{
            return -50;
        }
    }

    private int whereIsTok(String varName){

        int check = 0;

        for(int i = (symbolTable.size() - 1); i >= 0; i--){
            check = symbolTable.elementAt(i).indexOf(varName);

            if(check != -1){
            return i;
            }
        }

        return -20;
    }

    // Checks if variable is in the specified scope
    private boolean isInScope(int scope){

        int stackPos = (symbolTable.size() - 1) - scope;
        int exists = 0;

        if(stackPos < 0){
            return false;
        }
        else{
            exists = symbolTable.elementAt(stackPos).indexOf(tok.string);

            if(exists == -1){
                return false;
            }
            else{
                return true;
            }

        }
    }


}

