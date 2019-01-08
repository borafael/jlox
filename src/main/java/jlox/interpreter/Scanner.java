package jlox.interpreter;

import static jlox.interpreter.TokenType.AND;
import static jlox.interpreter.TokenType.BANG;
import static jlox.interpreter.TokenType.BANG_EQUAL;
import static jlox.interpreter.TokenType.CLASS;
import static jlox.interpreter.TokenType.COMMA;
import static jlox.interpreter.TokenType.DOT;
import static jlox.interpreter.TokenType.ELSE;
import static jlox.interpreter.TokenType.EOF;
import static jlox.interpreter.TokenType.EQUAL;
import static jlox.interpreter.TokenType.EQUAL_EQUAL;
import static jlox.interpreter.TokenType.FALSE;
import static jlox.interpreter.TokenType.FOR;
import static jlox.interpreter.TokenType.FUN;
import static jlox.interpreter.TokenType.GREATER;
import static jlox.interpreter.TokenType.GREATER_EQUAL;
import static jlox.interpreter.TokenType.IDENTIFIER;
import static jlox.interpreter.TokenType.IF;
import static jlox.interpreter.TokenType.LEFT_BRACE;
import static jlox.interpreter.TokenType.LEFT_PAREN;
import static jlox.interpreter.TokenType.LESS;
import static jlox.interpreter.TokenType.LESS_EQUAL;
import static jlox.interpreter.TokenType.MINUS;
import static jlox.interpreter.TokenType.NIL;
import static jlox.interpreter.TokenType.NUMBER;
import static jlox.interpreter.TokenType.OR;
import static jlox.interpreter.TokenType.PLUS;
import static jlox.interpreter.TokenType.PRINT;
import static jlox.interpreter.TokenType.RETURN;
import static jlox.interpreter.TokenType.RIGHT_BRACE;
import static jlox.interpreter.TokenType.RIGHT_PAREN;
import static jlox.interpreter.TokenType.SEMICOLON;
import static jlox.interpreter.TokenType.SLASH;
import static jlox.interpreter.TokenType.STAR;
import static jlox.interpreter.TokenType.STRING;
import static jlox.interpreter.TokenType.SUPER;
import static jlox.interpreter.TokenType.THIS;
import static jlox.interpreter.TokenType.TRUE;
import static jlox.interpreter.TokenType.VAR;
import static jlox.interpreter.TokenType.WHILE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Scanner {

	private static final Map<String, TokenType> keywords;

	static {
		keywords = new HashMap<>();
		keywords.put("and", AND);
		keywords.put("class", CLASS);
		keywords.put("else", ELSE);
		keywords.put("false", FALSE);
		keywords.put("for", FOR);
		keywords.put("fun", FUN);
		keywords.put("if", IF);
		keywords.put("nil", NIL);
		keywords.put("or", OR);
		keywords.put("print", PRINT);
		keywords.put("return", RETURN);
		keywords.put("super", SUPER);
		keywords.put("this", THIS);
		keywords.put("true", TRUE);
		keywords.put("var", VAR);
		keywords.put("while", WHILE);
	}

	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0;
	private int current = 0;
	private int line = 1;

	Scanner(String source) {
		this.source = source;
	}

	List<Token> scanTokens() {
		while (!isAtEnd()) {
			// We are at the beginning of the next lexeme.
			start = current;
			scanToken();
		}

		tokens.add(new Token(EOF, "", null, line));
		return tokens;
	}

	private boolean isAtEnd() {
		return current >= source.length();
	}

	private void scanToken() {
		char c = advance();
		switch (c) {
		case '(':
			addToken(LEFT_PAREN);
			break;
		case ')':
			addToken(RIGHT_PAREN);
			break;
		case '{':
			addToken(LEFT_BRACE);
			break;
		case '}':
			addToken(RIGHT_BRACE);
			break;
		case ',':
			addToken(COMMA);
			break;
		case '.':
			addToken(DOT);
			break;
		case '-':
			addToken(MINUS);
			break;
		case '+':
			addToken(PLUS);
			break;
		case ';':
			addToken(SEMICOLON);
			break;
		case '*':
			addToken(STAR);
			break;
		case '!':
			addToken(match('=') ? BANG_EQUAL : BANG);
			break;
		case '=':
			addToken(match('=') ? EQUAL_EQUAL : EQUAL);
			break;
		case '<':
			addToken(match('=') ? LESS_EQUAL : LESS);
			break;
		case '>':
			addToken(match('=') ? GREATER_EQUAL : GREATER);
			break;
		case '/':
			if (match('/')) {
				// A comment goes until the end of the line.
				while (peek() != '\n' && !isAtEnd())
					advance();
			} else {
				addToken(SLASH);
			}
			break;
		case ' ':
		case '\r':
		case '\t':
			// Ignore whitespace.
			break;
		case '\n':
			line++;
			break;
		case '"':
			string();
			break;
		default:
			if (isDigit(c)) {
				number();
			} else if (isAlpha(c)) {
				identifier();
			} else {
				Lox.error(line, "Unexpected character.");
			}
			break;
		}
	}

	private void identifier() {
		while (isAlphaNumeric(peek()))
			advance();

		// See if the identifier is a reserved word.
		String text = source.substring(start, current);

		TokenType type = keywords.get(text);
		if (type == null)
			type = IDENTIFIER;
		addToken(type);
	}

	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
	}

	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}

	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private void number() {
		while (isDigit(peek()))
			advance();

		// Look for a fractional part.
		if (peek() == '.' && isDigit(peekNext())) {
			// Consume the "."
			advance();

			while (isDigit(peek()))
				advance();
		}

		addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
	}

	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			if (peek() == '\n')
				line++;
			advance();
		}

		// Unterminated string.
		if (isAtEnd()) {
			Lox.error(line, "Unterminated string.");
			return;
		}

		// The closing ".
		advance();

		// Trim the surrounding quotes.
		String value = source.substring(start + 1, current - 1);
		addToken(STRING, value);
	}

	private char peek() {
		if (isAtEnd())
			return '\0';
		return source.charAt(current);
	}

	private char peekNext() {
		if (current + 1 >= source.length())
			return '\0';
		return source.charAt(current + 1);
	}

	private boolean match(char expected) {
		if (isAtEnd())
			return false;
		if (source.charAt(current) != expected)
			return false;

		current++;
		return true;
	}

	private char advance() {
		current++;
		return source.charAt(current - 1);
	}

	private void addToken(TokenType type) {
		addToken(type, null);
	}

	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line));
	}
}