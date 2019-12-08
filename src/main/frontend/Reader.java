
package main.frontend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;

public class Reader {

    private int numOfLine;
    private int charPosition;
    private BufferedReader buffer;
    private String line;

    public Reader(String path) throws FileNotFoundException {
        File file = new File(path);
        FileReader fr = new FileReader(file);
        buffer = new BufferedReader(fr);
    }

    public void openFile() throws IOException {
        line = buffer.readLine();
        numOfLine = 1;
        charPosition = 0;
    }

    public Character getCurrentChar() {
        if (line == null) {
            return '~';
        }
        if (charPosition >= line.length()) {
            return '#';
        }
        return line.charAt(charPosition);
    }

    public Character getNextChar() {
        charPosition++;
        if (charPosition >= line.length()) {
            return '#';
        }
        return line.charAt(charPosition);
    }

    public void getNextLine() throws IOException {
        line = buffer.readLine();
        if (line != null) {
            numOfLine++;
            charPosition = 0;
            while (line != null && line.trim().length() == 0) {
                line = buffer.readLine();
                numOfLine++;
            }
        }
    }

    public void closeFile() throws IOException {
        buffer.close();
    }

    public int getNumOfLine() {
        return numOfLine;
    }

    public int getCharPosition() {
        return charPosition;
    }

}
