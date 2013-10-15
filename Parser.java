/* *** This file is given as part of the programming assignment. *** */

import java.util.LinkedList;
import java.util.Stack;


public class Parser {

    private static final TK[] STATEMENT_ARR = {TK.TILDE, TK.ID, TK.PRINT, TK.DO, TK.IF};
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
        LinkedList<String> globals = new LinkedList<String>();
        // globals.add("Stuff");
        // globals.add("Junk");
        symbolTable.push(globals);
        // symbolTable.elementAt(0).add("Hereweare");
        // System.out.println(symbolTable.elementAt(0));
        block();
    }

    private void block(){
        symbolTable.push(new LinkedList<String>());
        declaration_list();
        statement_list();
        symbolTable.pop();
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

        int stackPos = symbolTable.size() - 1;

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
        else{
            // System.err.println (tok + " error in statement();");
        }

    }

    private void print() {
        // System.err.println (tok + " in print()");
        mustbe(TK.PRINT);
        expr();
    }

    private void assigment() {
        // System.err.println (tok + " in assignment()");
        ref_id();
        mustbe(TK.ASSIGN);
        expr();
    }

    private void ref_id() {

        int scopeModifier = 0;

        if( is(TK.TILDE)){
            mustbe(TK.TILDE);

            if( is(TK.NUM)){
                scopeModifier = Integer.parseInt(tok.string);
                mustbe(TK.NUM);
            }
        }
        // System.err.println (tok + " in ref_id()");

        // symbolTable.elementAt(stackPos).add(tok.string);
        // System.out.println(symbolTable);


        if(!isInSymbolTable(tok)){
            System.err.println(tok.string + " is an undeclared variable on line "
                                + tok.lineNumber);
            // System.out.println(symbolTable);
            System.exit(1);
        }
        mustbe(TK.ID);

    }

    private void expr() {
        term();
        while( isMany(ADDOP_ARR) ){
            if(is(TK.PLUS)){
                mustbe(TK.PLUS);
            }
            else{
                mustbe(TK.MINUS);
            }

            term();
        }
    }

    private void term() {
        factor();
        while( isMany(MULTOP_ARR) ){
            if(is(TK.TIMES)){
                mustbe(TK.TIMES);
            }
            else{
                mustbe(TK.DIVIDE);
            }

            factor();
        }

    }

    private void factor() {
        if( is(TK.LPAREN) ){
            mustbe(TK.LPAREN);
            expr();
            mustbe(TK.RPAREN);
        }
        else if( is(TK.NUM) ){
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
        guarded_command();
        mustbe(TK.ENDDO);
    }

    private void e_If() {
        // System.err.println (tok + " in e_If()");
        mustbe(TK.IF);
        guarded_command();
        while(is(TK.ELSEIF)){
            mustbe(TK.ELSEIF);
            guarded_command();
        }

        if(is(TK.ELSE)){
            mustbe(TK.ELSE);
            block();
        }

        mustbe(TK.ENDIF);



        // System.err.println (tok + " in e_If()");

    }

    private void guarded_command() {
        // System.err.println (tok + " in guarded_command()");
        expr();
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

    private void addToSymbolTable(){
        int stackPos = symbolTable.size() - 1;
        Token tempTok = tok;

        mustbe(TK.ID);

        if(!isInLatestScope(tempTok)){
            symbolTable.elementAt(stackPos).add(tempTok.string);
            // System.out.println(symbolTable);
        }
        else{
            System.err.println("redeclaration of variable " + tempTok.string);
        }

    }

    private boolean isInLatestScope(Token theTok){

        int stackPos = symbolTable.size() - 1;
        int exists = symbolTable.elementAt(stackPos).indexOf(theTok.string);

        if(exists == -1){
            return false;
        }
        else{
            return true;
        }   
    }

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


}

