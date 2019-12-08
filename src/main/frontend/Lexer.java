
package main.frontend;

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

public class Lexer {

    private Reader reader;
    private Character curr;
    private int val;
    private int id;
    public static List<String> ids;

    public Lexer(String fileName) throws IOException {
        reader = new Reader(fileName);
        curr = null;
        id = -1;
        ids = new ArrayList<>();
        ids.add("InputNum");
        ids.add("OutputNum");
        ids.add("OutputNewLine");
    }

    public void startScanner() throws IOException {
        reader.openFile();
        curr = reader.getCurrentChar();
    }

    public void closeScanner() throws IOException {
        reader.closeFile();
    }

    private void nextChar() {
        curr = reader.getNextChar();
    }

    private void nextLine() throws IOException {
        reader.getNextLine();
        curr = reader.getCurrentChar();
    }

    public Token getNextToken() throws IOException {
        Token curToken = skipSpaceAndComment();
        if (curToken != null) {
            return curToken;
        }
        if (curr == '~') {
            return Token.EOF;
        }
        if ((curToken = getNumberToken()) != null) {
            return curToken;
        }
        if ((curToken = getToken()) != null) {
            return curToken;
        }
        switch (curr) {
            case '+':
                nextChar();
                return Token.PLUS;
            case '-':
                nextChar();
                return Token.MINUS;
            case '*':
                nextChar();
                return Token.TIMES;
            case '=':
                nextChar();
                if (curr == '=') {
                    nextChar();
                    return Token.EQL;
                }
                else {
                    this.Error("'=' SHOULD FOLLOWED BY '='!");
                    return Token.ERROR;
                }
            case '!':
                nextChar();
                if (curr == '=') {
                    nextChar();
                    return Token.NEQ;
                }
                else {
                    this.Error("'!' SHOULD FOLLOWED BY '='!");
                    return Token.ERROR;
                }
            case '>':
                nextChar();
                if (curr == '=') {
                    nextChar();
                    return Token.GEQ;
                }
                else {
                    return Token.GRE;
                }
            case '<':
                nextChar();
                if (curr == '=') {
                    nextChar();
                    return Token.LEQ;
                }
                else if (curr == '-'){
                    nextChar();
                    return Token.BECOMES;
                }
                else {
                    return Token.LSS;
                }
            case '.':
                nextChar();
                return Token.PERIOD;
            case ',':
                nextChar();
                return Token.COMMA;
            case ';':
                nextChar();
                return Token.SEMICOLON;
            case ':':
                nextChar();
                return Token.COLON;
            case '(':
                nextChar();
                return Token.OPEN_PARENTHESIS;
            case ')':
                nextChar();
                return Token.CLOSE_PARENTHESIS;
            case '[':
                nextChar();
                return Token.OPEN_BRACKET;
            case ']':
                nextChar();
                return Token.CLOSE_BRACKET;
            case '{':
                nextChar();
                return Token.OPEN_BRACE;
            case '}':
                nextChar();
                return Token.CLOSE_BRACE;
        }
        return Token.ERROR;
    }

    public Token skipSpaceAndComment() throws IOException {
        while (curr == '\t' || curr == '\r' || curr == '\n' || curr == ' ' || curr == '#' || curr == '/') {
            if (curr == '\t' || curr == '\r' || curr == '\n' || curr == ' ') {
                nextChar();
            }
            else if (curr == '#') {
                nextLine();
            }
            else {
                nextChar();
                if (curr == '/') {
                    nextLine();
                }
                else {
                    return Token.DIVIDE;
                }
            }
        }
        return null;
    }

    public Token getNumberToken() {
        boolean isNumber = false;
        val = 0;
        while (curr >= '0' && curr <= '9') {
            isNumber = true;
            val = 10 * val + curr - '0';
            nextChar();
        }
        return isNumber ? Token.NUMBER : null;
    }

    public Token getToken() {
        boolean isLetter = false;
        StringBuilder sb = new StringBuilder();
        while (Character.isLetterOrDigit(curr)) {
            if (!isLetter && Character.isDigit(curr)) {
                Error("INVALID IDENTIFIER!");
                return null;
            }
            isLetter = true;
            sb.append(curr);
            nextChar();
        }
        if (!isLetter) {
            return null;
        }
        String tokenString = sb.toString();
        Token Keyword = buildKeyWord(tokenString);
        if (Keyword != null) {
            return Keyword;
        }
        if (!ids.contains(tokenString)) {
            ids.add(tokenString);
        }
        id = ids.indexOf(tokenString);
        return Token.IDENTIFIER;
    }

    private Token buildKeyWord(String tokenString) {
        return Token.buildToken(tokenString);
    }

    public void Error(String msg) {
        System.err.println("LEXER ERROR - SYNTAX ERROR AT " + reader.getNumOfLine() + ": " + msg);
    }

    public int getLineNumber() {
        return reader.getNumOfLine();
    }

    public int getVarID() {
        return id;
    }

    public int getVal() {
        return val;
    }

    public int getID() {
        return id;
    }

}
