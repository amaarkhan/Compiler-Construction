# Compiler-Construction
Overview

This project is a custom compiler for a simple programming language. The compiler is designed to perform lexical analysis, tokenization, NFA and DFA construction, symbol table generation, and basic syntax parsing (e.g., if-else statements). This document serves as a user manual to help you understand the language's syntax, rules, and how to use the compiler.
Language Features

The custom language supports the following features:

    Variables: Declare and assign values to variables.

    Data Types: Integers, floating-point numbers, booleans, characters, and strings.

    Control Structures: if, else, and while loops.

    Input/Output: Print statements using say.

    Expressions: Arithmetic and boolean expressions.

    Comments: Single-line and multi-line comments.

Keywords

The language supports the following keywords:
Keyword:	Description
whole:	Declares an integer variable.
fraction:	Declares a floating-point variable.
truth:	Declares a boolean variable.
character:	Declares a character variable.
while:	Starts a while loop.
if:	Starts an if statement.
else:	Starts an else block.
end:	Marks the end of a block (e.g., loop or conditional).
say:	Prints output to the console.
Rules and Syntax
1. Identifiers

    Must start with a lowercase letter 

    Can contain lowercase letters, digits

    Uppercase letters are not allowed.

    Examples:

        Valid: x,  num1

        Invalid: X, MyVar, 1num

2. Literals

    Numbers:

        Integers: 42, 100

        Floating-point: 3.14, 0.5

    Strings: Enclosed in double quotes (e.g., "Hello, World!").

    Characters: Enclosed in single quotes (e.g., 'a').

    Booleans: yes or no.

3. Operators

    Arithmetic: +, -, *, /, %, ^ (exponentiation).

    Comparison: ==, !=, <, >, <=, >=.

    Assignment: ->.

4. Expressions

    Mathematical expressions follow standard operator precedence:

        Parentheses () have the highest precedence.

        Exponentiation ^ comes next.

        Multiplication *, division /, and modulus % have the same precedence.

        Addition + and subtraction - have the lowest precedence.

    Boolean expressions can use && (AND), || (OR), and comparison operators.

5. Blocks

    Code blocks are enclosed in curly braces {}.

    Statements must end with a semicolon ;.

6. Comments

    Single-line comments start with //.

    Multi-line comments are enclosed in /* ... */.

Example Program

Below is an example program written in the custom language:
plaintext
Copy

// Declare variables
whole x -> 10;
fraction y -> 3.14;
truth flag -> yes;

// If-else statement
if (x > 5) {
    say "x is greater than 5";
} else {
    say "x is less than or equal to 5";
}

// While loop
while (x > 0) {
    say x;
    x -> x - 1;
}

Compiler Workflow

The compiler performs the following steps:
1. Lexical Analysis

    Tokenizes the input source code into keywords, identifiers, literals, operators, and punctuation.

    Example: whole x -> 10; is tokenized as:

        whole (Keyword)

        x (Identifier)

        -> (Assignment Operator)

        10 (Number)

        ; (Punctuation)

2. NFA Construction

    Builds a Non-Deterministic Finite Automaton (NFA) to validate tokens.

    The NFA is constructed based on the language's rules and syntax.

3. DFA Construction

    Converts the NFA to a Deterministic Finite Automaton (DFA) for efficient token matching.

    The DFA ensures that all tokens are valid according to the language's rules.

4. Symbol Table Generation

    Tracks variables, their types, scopes, and memory locations.

    Example:

        x (whole, global, value = 10, memory = M1000)

        y (fraction, global, value = 3.14, memory = M1001)

5. Syntax Parsing

    Validates if-else statements and other basic syntax.

    Ensures that all blocks are properly closed and conditions are valid.

How to Use the Compiler
Step 1: Prepare Your Code

    Write your program in the custom language.

    Save the file as code.kh in the same directory as the compiler.

Step 2: Run the Compiler

    Compile and run the Compiler.java file.

    The compiler will output:

        Tokenized input.

        NFA and DFA states.

        Symbol table.

        Syntax validation results.

Step 3: Review Output

    Check the console for any errors or warnings.

    Verify that the symbol table and syntax parsing are correct.

Error Handling

The compiler provides detailed error messages for:

    Invalid Tokens: Tokens that do not match the language's rules.

    Syntax Errors: Missing braces, invalid conditions, or incorrect assignments.

    Invalid Variable Declarations: Variables declared with invalid names or types.
