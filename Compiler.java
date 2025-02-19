import java.util.*;
import java.util.regex.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;


// ========================== Regular Expressions (RE) Definition ==========================
class RegularExpressions {
   public static final String KEYWORDS = "\\b(whole|fraction|truth|character|while|if|else|end|say)\\b";

    public static final String IDENTIFIER = "\\b[a-z_][a-z0-9_]*\\b";  // Identifiers must be lowercase only
   public static final String NUMBER = "\\d+(\\.\\d{1,5})?(\\^\\d+(\\.\\d{1,5})?)?";

    public static final String OPERATORS = "->|\\+|-|\\*|/|%|\\^";
    public static final String COMPARISON_OPERATORS = "<=|>=|==|!=|<|>";
    public static final String BRACKETS = "\\[|\\]|\\{|\\}|\\(|\\)|;|:";
    public static final String STRING_LITERAL = "\\\"[^\\\"]*\\\"";
    public static final String CHARACTER_LITERAL = "\\'[^\\']\\'";
    public static final String BOOLEAN_LITERAL = "yes|no";
    public static final String COMMENTS = "//.*|/\\*[\\s\\S]*?\\*/";
}

class Token {
    String value;
    String type;

    public Token(String value, String type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("%-20s -> %s", value, type);
    }
}

class Tokenizer {
    public static List<Token> tokenize(String code) throws Exception {
        List<Token> tokens = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        // Preprocessing: Remove comments
        code = code.replaceAll(RegularExpressions.COMMENTS, "");

        // Tokenization regex
        String regex = RegularExpressions.KEYWORDS + "|" +
                       RegularExpressions.IDENTIFIER + "|" +
                       RegularExpressions.NUMBER + "|" +
                       RegularExpressions.OPERATORS + "|" +
                       RegularExpressions.COMPARISON_OPERATORS + "|" +
                       RegularExpressions.STRING_LITERAL + "|" +
                       RegularExpressions.CHARACTER_LITERAL + "|" +
                       RegularExpressions.BOOLEAN_LITERAL + "|" +
                       "[{}();,:]";

        Pattern pattern = Pattern.compile(regex);
        
        // Read input **character by character** to prevent splitting invalid tokens
        int lineNumber = 1;
        int tokenCount = 0;
        
        String[] lines = code.split("\n");
        
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            int lastMatchEnd = 0;
            
            while (matcher.find()) {
                String token = matcher.group();
                int start = matcher.start();
                
                // Check if there is an invalid sequence between last match and current match
                if (lastMatchEnd < start) {
                    String invalidToken = line.substring(lastMatchEnd, start).trim();
                    if (!invalidToken.isEmpty()) {
                        errors.add("❌ Invalid Token: '" + invalidToken + "' (Line: " + lineNumber + ", Token: " + (tokenCount + 1) + ")");
                    }
                }

                // Check for uppercase identifiers (invalid as per your rule)
                if (token.matches("[A-Z_][A-Za-z0-9_]*")) {
                    errors.add("❌ Invalid Token: '" + token + "' (Line: " + lineNumber + ", Token: " + (tokenCount + 1) + ")");
                } else {
                    tokens.add(new Token(token, classifyToken(token)));
                    tokenCount++;
                }

                lastMatchEnd = matcher.end();
            }

            // Check for leftover invalid tokens at the end of the line
            if (lastMatchEnd < line.length()) {
                String invalidToken = line.substring(lastMatchEnd).trim();
                if (!invalidToken.isEmpty()) {
                    errors.add("❌ Invalid Token: '" + invalidToken + "' (Line: " + lineNumber + ", Token: " + (tokenCount + 1) + ")");
                }
            }

            lineNumber++;
        }

        // Print lexical errors and halt compilation if errors exist
        if (!errors.isEmpty()) {
            System.err.println("\n🔴 Lexical Errors:");
            for (String error : errors) {
                System.err.println(error);
            }
            throw new Exception("Lexical analysis failed due to invalid tokens.");
        }

        return tokens;
    }

    private static String classifyToken(String token) {
        if (token.matches(RegularExpressions.KEYWORDS)) return "Keyword";
        if (token.matches(RegularExpressions.IDENTIFIER)) return "Identifier";
        if (token.matches(RegularExpressions.NUMBER)) return "Constant";
        if (token.matches(RegularExpressions.STRING_LITERAL) || token.matches(RegularExpressions.CHARACTER_LITERAL)) return "Literal";
        if (token.matches(RegularExpressions.BOOLEAN_LITERAL)) return "Boolean Constant";
        if (token.matches(RegularExpressions.OPERATORS)) return "Operator";
        if (token.matches(RegularExpressions.COMPARISON_OPERATORS)) return "Comparison Operator";
        if (token.matches("->")) return "Assignment Operator";
        if (token.matches("[{}();,:]")) return "Punctuation";
        
        return "Unknown";
    }
}



// ========================== NFA State Representation ==========================
class NFAState {
    String name;
    Map<Character, List<NFAState>> transitions = new HashMap<>();
    boolean isFinal;

    public NFAState(String name) {
        this.name = name;
        this.isFinal = false;
    }

    public void addTransition(char input, NFAState nextState) {
        transitions.putIfAbsent(input, new ArrayList<>());
        transitions.get(input).add(nextState);
    }
    
}

// ========================== Optimized NFA ==========================
class NFA {
    private NFAState startState;
    private Set<NFAState> allStates = new HashSet<>();
    private Set<NFAState> finalStates = new HashSet<>();
    private Map<String, NFAState> stateMap = new HashMap<>();

    public NFA(List<String> tokens) {
        startState = getOrCreateState("q0");
        for (String token : tokens) {
            addToken(token);
        }
    }
    // Add this getter method inside the NFA class
public NFAState getStartState() {
    return startState;
}


    private NFAState getOrCreateState(String name) {
        return stateMap.computeIfAbsent(name, k -> {
            NFAState state = new NFAState(name);
            allStates.add(state);
            return state;
        });
    }

private void addToken(String token) {
    NFAState current = startState;
    for (char c : token.toCharArray()) {
        String nextStateName = current.name + "_" + c;
        NFAState next = getOrCreateState(nextStateName);
        current.addTransition(c, next);
        current = next;
    }
    current.isFinal = true;
    finalStates.add(current);
}

    
    
    
    public void printUniqueStates() {
    Set<String> uniqueStateNames = new HashSet<>();
    for (NFAState state : allStates) {
        uniqueStateNames.add(state.name);
    }

    System.out.println("\n🟢 Unique NFA States:");
    for (String state : uniqueStateNames) {
        System.out.println("  - " + state);
    }
    System.out.println("======================================================");
}


public void printNFAInfo() {
    System.out.println("\n============== STEP 2: NFA CONSTRUCTION ==============");
    System.out.println("🔵 Total NFA States: " + allStates.size());
    System.out.println("🔹 Final States: " + finalStates.size());

    System.out.println("\n📜 NFA Transition Table:");
    System.out.printf("%-25s %-10s %-25s\n", "State", "Input", "Next State(s)");
    System.out.println("-------------------------------------------------------------");

    for (NFAState state : allStates) {
        for (Map.Entry<Character, List<NFAState>> entry : state.transitions.entrySet()) {
            String nextStates = entry.getValue().stream()
                    .map(n -> n.name.replace("_ ", "_"))  // Remove unwanted spaces
                    .distinct()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("None");

            // Mark final states explicitly
            String stateDisplay = state.isFinal ? state.name + " (FINAL)" : state.name;

            System.out.printf("%-25s %-10s %-25s\n", stateDisplay, entry.getKey(), nextStates);
        }
    }
    System.out.println("======================================================");
}






}
// ========================== DFA Implementation ==========================
// ========================== Optimized DFA ==========================
class DFA {
    private Set<Set<NFAState>> dfaStates = new HashSet<>();
    private Map<Set<NFAState>, Map<Character, Set<NFAState>>> transitions = new HashMap<>();
    private Set<Set<NFAState>> finalStates = new HashSet<>();
    private Set<NFAState> startState;

    public DFA(NFA nfa) {
        // Use the getter method instead of directly accessing private variables
this.startState = epsilonClosure(Collections.singleton(nfa.getStartState()));

        constructDFA();
    }

    private Set<NFAState> epsilonClosure(Set<NFAState> states) {
        Set<NFAState> closure = new HashSet<>(states);
        return closure;
    }

    private void constructDFA() {
        Queue<Set<NFAState>> queue = new LinkedList<>();
        queue.add(startState);
        dfaStates.add(startState);

        while (!queue.isEmpty()) {
            Set<NFAState> currentDFAState = queue.poll();
            Map<Character, Set<NFAState>> newTransitions = new HashMap<>();

            for (NFAState nfaState : currentDFAState) {
                for (Map.Entry<Character, List<NFAState>> entry : nfaState.transitions.entrySet()) {
                    char input = entry.getKey();
                    Set<NFAState> nextState = new HashSet<>(entry.getValue());

                    if (!dfaStates.contains(nextState)) {
                        dfaStates.add(nextState);
                        queue.add(nextState);
                    }
                    newTransitions.put(input, nextState);
                }
            }
            transitions.put(currentDFAState, newTransitions);
        }

        for (Set<NFAState> stateSet : dfaStates) {
            for (NFAState state : stateSet) {
                if (state.isFinal) {
                    finalStates.add(stateSet);
                    break;
                }
            }
        }
    }

public boolean matchToken(String token) {
    Set<NFAState> currentState = startState;
    for (char c : token.toCharArray()) {
        currentState = transitions.getOrDefault(currentState, new HashMap<>()).get(c);
        if (currentState == null) return false;
    }
    return finalStates.contains(currentState);
}



public void printUniqueDFAStates() {
    Set<String> uniqueStateNames = new HashSet<>();
    for (Set<NFAState> stateSet : dfaStates) {
        String stateName = stateSet.toString().replaceAll("[\\[\\]]", "");  // Remove brackets for clarity
        uniqueStateNames.add(stateName);
    }

    System.out.println("\n🟢 Unique DFA States:");
    for (String state : uniqueStateNames) {
        System.out.println("  - " + state);
    }
    System.out.println("======================================================");
}

public void printDFAInfo() {
    System.out.println("\n============== STEP 3: DFA CONSTRUCTION ==============");
    System.out.println("🔵 Total DFA States: " + dfaStates.size());

    printUniqueDFAStates();  // Call the new method to display unique states

    System.out.println("\n📜 DFA Transition Table:");
    System.out.printf("%-20s %-10s %-20s\n", "State", "Input", "Next State");
    System.out.println("----------------------------------------------------------");

    for (Map.Entry<Set<NFAState>, Map<Character, Set<NFAState>>> entry : transitions.entrySet()) {
        String currentStateName = entry.getKey().toString();
        for (Map.Entry<Character, Set<NFAState>> trans : entry.getValue().entrySet()) {
            String nextState = trans.getValue().toString().replaceAll("[\\[\\]]", ""); // Remove brackets for clarity
            System.out.printf("%-20s %-10s %-20s\n", currentStateName, trans.getKey(), nextState);
        }
    }
    System.out.println("======================================================");
}


}

class ExpressionEvaluator {
    private static final Map<String, Integer> precedence = new HashMap<>();

    static {
        precedence.put("+", 1);
        precedence.put("-", 1);
        precedence.put("*", 2);
        precedence.put("/", 2);
        precedence.put("%", 2);
        precedence.put("^", 3); // Highest precedence for exponentiation
    }

    public static double evaluate(String expression) {
        expression = expression.replaceAll("\\s", ""); // Remove spaces

        if (!isValidExpression(expression)) {
            throw new IllegalArgumentException("Invalid arithmetic expression: " + expression);
        }

        return evaluatePostfix(infixToPostfix(expression));
    }

    private static boolean isValidExpression(String expression) {
        return expression.matches(".*\\d.*") && expression.matches(".*[+\\-*/%^()].*"); // Ensures valid mathematical format
    }

    private static List<String> infixToPostfix(String expression) {
        List<String> output = new ArrayList<>();
        Stack<String> operators = new Stack<>();
        Stack<Character> parentheses = new Stack<>();
        StringTokenizer tokenizer = new StringTokenizer(expression, "()+-*/%^", true);

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (token.isEmpty()) continue;

            if (token.matches("\\d+(\\.\\d+)?")) {
                output.add(token); // Operand (Number)
            } else if (token.equals("(")) {
                operators.push(token);
                parentheses.push('(');
            } else if (token.equals(")")) {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    output.add(operators.pop());
                }
                if (!operators.isEmpty() && operators.peek().equals("(")) {
                    operators.pop(); // Remove '(' from stack
                }
                if (!parentheses.isEmpty()) parentheses.pop();
            } else {
                while (!operators.isEmpty() && precedence.containsKey(operators.peek()) &&
                        precedence.get(operators.peek()) >= precedence.get(token)) {
                    output.add(operators.pop());
                }
                operators.push(token);
            }
        }

        while (!operators.isEmpty()) {
            output.add(operators.pop());
        }

        return output;
    }

// ✅ Now, add this inside SymbolTable:
private static double roundToFiveDecimals(double value) {
    BigDecimal bd = new BigDecimal(value).setScale(5, RoundingMode.HALF_UP);
    return bd.doubleValue();
}


private static double evaluatePostfix(List<String> postfix) {
    Stack<Double> stack = new Stack<>();

    for (String token : postfix) {
        if (token.matches("\\d+(\\.\\d+)?")) {
            stack.push(Double.parseDouble(token));
        } else {
            double b = stack.pop();
            double a = stack.pop();
            double result;

            switch (token) {
                case "+": result = a + b; break;
                case "-": result = a - b; break;
                case "*": result = a * b; break;
                case "/": result = a / b; break;
                case "%": result = a % b; break;
                case "^": result = Math.pow(a, b); break;
                default: throw new IllegalArgumentException("Unknown operator: " + token);
            }
            
            // ✅ Ensure correct rounding to 5 decimal places
            stack.push(roundToFiveDecimals(result));
        }
    }
    return stack.pop();
}


}



class SymbolTable {
    private Map<String, SymbolTableEntry> table = new HashMap<>();
    private int memoryCounter = 1000;



public void addEntry(String name, String type, String scope, List<Token> tokens, int startIndex, int lineNumber) {
    StringBuilder expression = new StringBuilder();
    
    int i = startIndex;
    boolean foundAssignment = false;

    while (i < tokens.size() && !tokens.get(i).value.equals(";")) {
        if (tokens.get(i).value.equals("->")) {
            foundAssignment = true;
        } else if (foundAssignment) {
            expression.append(tokens.get(i).value);
        }
        i++;
    }

    String value = expression.toString().trim();

    if (isMathematicalExpression(value)) {
        try {
            value = String.valueOf(roundToFiveDecimals(ExpressionEvaluator.evaluate(value)));  // ✅ Correctly rounds before storing
        } catch (Exception e) {
            System.err.println("❌ Error: Invalid arithmetic expression in '" + value + "'");
            value = "ERROR";
        }
    } else if (isValidLiteral(value)) {
        if (value.matches("\\d+\\.\\d+")) { // ✅ If it's a decimal, round it
            value = String.valueOf(roundToFiveDecimals(Double.parseDouble(value)));
        }
    } else {
        System.err.println("❌ Error: Invalid assignment in '" + value + "'");
        value = "ERROR";
    }

    table.put(name, new SymbolTableEntry(name, type, scope, value, "M" + memoryCounter++, lineNumber));
}

// ✅ Now, add this inside SymbolTable:
private static double roundToFiveDecimals(double value) {
    BigDecimal bd = new BigDecimal(value).setScale(5, RoundingMode.HALF_UP);
    return bd.doubleValue();
}



    private boolean isMathematicalExpression(String value) {
        return value.matches(".*\\d.*") && value.matches(".*[+\\-*/%^()].*"); // Ensures it contains numbers & operators
    }

    private boolean isValidLiteral(String value) {
        return value.matches("\\d+(\\.\\d+)?") || // Numeric constant
               value.matches("'[^']'") ||       // Character constant ('a', 'z', etc.)
               value.matches("\"[^\"]*\"") ||    // String literal
               value.equals("yes") || value.equals("no"); // Boolean literals
    }

    public void printSymbolTable() {
        System.out.println("\n============== SYMBOL TABLE ==============");
        System.out.printf("%-15s %-10s %-10s %-15s %-10s %-10s\n", "Identifier", "Type", "Scope", "Value", "Memory", "Line");
        System.out.println("--------------------------------------------------------------");
        for (SymbolTableEntry entry : table.values()) {
            System.out.println(entry);
        }
        System.out.println("==============================================================");
    }
   

}

class IfElseParser {
    private List<Token> tokens;
    private int index = 0;

    public IfElseParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public void parse() throws Exception {
        while (index < tokens.size()) {
            Token token = tokens.get(index);
            if (token.value.equals("if")) {
                parseIfStatement();
            } else {
                index++;
            }
        }
    }

    private void parseIfStatement() throws Exception {
        System.out.println("🔍 Parsing `if` statement at token index: " + index);

        // Check for '(' after 'if'
        if (++index >= tokens.size() || !tokens.get(index).value.equals("(")) {
            throw new Exception("❌ Syntax Error: Expected '(' after `if` at token " + index);
        }

        // Extract full condition inside `if` (multi-token condition)
        index++;
        StringBuilder condition = new StringBuilder();
        while (index < tokens.size() && !tokens.get(index).value.equals(")")) {
            condition.append(tokens.get(index).value).append(" ");
            index++;
        }

        // Check for closing ')'
        if (index >= tokens.size() || !tokens.get(index).value.equals(")")) {
            throw new Exception("❌ Syntax Error: Expected ')' after condition at token " + index);
        }

        // Validate the full condition as a Boolean expression
        if (!isBooleanExpression(condition.toString().trim())) {
            throw new Exception("❌ Syntax Error: Invalid condition in `if` statement at token " + index);
        }

        // Check for opening '{'
        if (++index >= tokens.size() || !tokens.get(index).value.equals("{")) {
            throw new Exception("❌ Syntax Error: Expected '{' after `if` condition at token " + index);
        }

        // Parse the block inside `if`
        parseBlock();

        // Check for `else`
        if (index < tokens.size() && tokens.get(index).value.equals("else")) {
            index++;

            if (index < tokens.size() && tokens.get(index).value.equals("if")) {
                parseIfStatement(); // Handle `else if`
            } else if (index < tokens.size() && tokens.get(index).value.equals("{")) {
                parseBlock(); // Handle `else`
            } else {
                throw new Exception("❌ Syntax Error: Expected '{' after `else` at token " + index);
            }
        }
    }

    private void parseBlock() throws Exception {
        int openBraces = 1; // Already encountered one '{'
        index++;

        while (index < tokens.size() && openBraces > 0) {
            if (tokens.get(index).value.equals("{")) {
                openBraces++;
            } else if (tokens.get(index).value.equals("}")) {
                openBraces--;
            }
            index++;
        }

        if (openBraces != 0) {
            throw new Exception("❌ Syntax Error: Unmatched `{` in `if-else` block.");
        }
    }

  private boolean isBooleanExpression(String value) {
    return value.matches("\\b[a-z_][a-z0-9_]*\\b") ||  // ✅ NEW: Allow boolean variable alone
           value.matches(".*\\b[a-z_][a-z0-9_]*\\b\\s*(==|!=|>|<|>=|<=)\\s*(yes|no|\\d+(\\.\\d{1,5})?).*") ||
           value.matches(".*\\b\\d+(\\.\\d{1,5})?\\s*(==|!=|>|<|>=|<=)\\s*(\\d+(\\.\\d{1,5})?).*") ||
           value.matches(".*\\b[a-z_][a-z0-9_]*\\b\\s*(&&|\\|\\|)\\s*\\b[a-z_][a-z0-9_]*\\b.*");
}


}


class SymbolTableEntry {
    String name;
    String type;
    String scope;
    String value;
    String memoryLocation;
    int lineNumber;

    public SymbolTableEntry(String name, String type, String scope, String value, String memoryLocation, int lineNumber) {
        this.name = name;
        this.type = type;
        this.scope = scope;
        this.value = value;
        this.memoryLocation = memoryLocation;
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString() {
        return String.format("%-15s %-10s %-10s %-15s %-10s %-10d", name, type, scope, value, memoryLocation, lineNumber);
    }
}



public class Compiler {
    public static void main(String[] args) {
        try {
            System.out.println("\n=================================================");
            System.out.println("🚀 COMPILATION STARTED");
            System.out.println("=================================================");

            // Step 1: Read source code from file
            String sourceCode = new String(Files.readAllBytes(Paths.get("code.kh")));

            // Step 2: Tokenization
            System.out.println("\n🟢 STEP 1: LEXICAL ANALYSIS (TOKENIZATION)");
            List<Token> tokens = Tokenizer.tokenize(sourceCode);
            System.out.println("✅ Total Tokens Found: " + tokens.size());
            System.out.println("-------------------------------------------------");
            for (int i = 0; i < tokens.size(); i++) {
                System.out.printf("  %3d. %-20s -> %s\n", (i + 1), tokens.get(i).value, tokens.get(i).type);
            }
            System.out.println("=================================================");

            // Step 3: Build NFA
            System.out.println("\n🟠 STEP 2: NFA CONSTRUCTION");
            List<String> tokenValues = tokens.stream().map(t -> t.value).collect(Collectors.toList());
            NFA nfa = new NFA(tokenValues);
            nfa.printUniqueStates();
            nfa.printNFAInfo();
            System.out.println("=================================================");

            // Step 4: Convert NFA to DFA
            System.out.println("\n🔵 STEP 3: DFA CONSTRUCTION");
            DFA dfa = new DFA(nfa);
            dfa.printDFAInfo();
            System.out.println("=================================================");

            // Step 5: Token Validation using DFA
            System.out.println("\n🟣 STEP 4: TOKEN VALIDATION (DFA MATCHING)");
            System.out.printf("%-20s %-10s\n", "Token", "Validation");
            System.out.println("-------------------------------------------------");
            for (Token token : tokens) {
                boolean isValid = dfa.matchToken(token.value);
                System.out.printf("  %-20s %-10s\n", token.value, (isValid ? "✅ Valid" : "❌ Invalid"));
            }
            System.out.println("=================================================");

            // Step 6: Symbol Table Processing
            System.out.println("\n🟡 STEP 5: SYMBOL TABLE CONSTRUCTION");
            SymbolTable symbolTable = new SymbolTable();
            Stack<String> scopeStack = new Stack<>();
            scopeStack.push("global");  // Default scope

            for (int i = 0; i < tokens.size(); i++) {
                String currentScope = scopeStack.peek();

                if (tokens.get(i).value.equals("{")) {
                    scopeStack.push("local_" + i);
                } else if (tokens.get(i).value.equals("}")) {
                    scopeStack.pop();
                }

                if (tokens.get(i).type.equals("Keyword")) {
                    if (i + 2 < tokens.size() && tokens.get(i + 1).type.equals("Identifier")) {
                        symbolTable.addEntry(tokens.get(i + 1).value, tokens.get(i).value, currentScope, tokens, i + 2, i + 1);
                    }
                }
            }

            symbolTable.printSymbolTable();
            System.out.println("=================================================");

            // Step 7: If-Else Parsing
            System.out.println("\n🔴 STEP 6: IF-ELSE STATEMENT PARSING");
            IfElseParser ifElseParser = new IfElseParser(tokens);
            ifElseParser.parse();
            System.out.println("=================================================");

            // Compilation Success
            System.out.println("\n🎉 ✅ COMPILATION SUCCESSFUL ✅ 🎉");
            System.out.println("=================================================");

        } catch (Exception e) {
            System.out.println("\n🚨 ❌ COMPILATION ERROR ❌ 🚨");
            System.out.println("=================================================");
            System.err.println("🔴 ERROR: " + e.getMessage());
        }
    }
}


