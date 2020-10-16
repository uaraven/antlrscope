# Headlights

A simple UI for testing ANTLR4 grammars.

Supports Lexer and Combined grammars.

## Using

Headlights is a Java application, you will need Java 14 to run it.

Get the latest jar from the [releases](https://github.com/uaraven/headlights/releases) and run it with 

    java -jar headlights-<version-number>.jar
    
Copy grammar to the text area on the left and the text to parse to the text area on the right, then click "Parse" button. Boom. That's it. 

## Limitations

As Headlights uses Antlr in the interpreted mode it is impossible to execute any code defined in
the grammar, including predicates. The interpreter runs as if there is not predicates at all. 