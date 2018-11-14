package jlox;

import static jlox.TokenType.AND;
import static jlox.TokenType.BANG;
import static jlox.TokenType.BANG_EQUAL;
import static jlox.TokenType.CLASS;
import static jlox.TokenType.COMMA;
import static jlox.TokenType.DOT;
import static jlox.TokenType.ELSE;
import static jlox.TokenType.EOF;
import static jlox.TokenType.EQUAL;
import static jlox.TokenType.EQUAL_EQUAL;
import static jlox.TokenType.FALSE;
import static jlox.TokenType.FOR;
import static jlox.TokenType.FUN;
import static jlox.TokenType.GREATER;
import static jlox.TokenType.GREATER_EQUAL;
import static jlox.TokenType.IDENTIFIER;
import static jlox.TokenType.IF;
import static jlox.TokenType.LEFT_BRACE;
import static jlox.TokenType.LEFT_PAREN;
import static jlox.TokenType.LESS;
import static jlox.TokenType.LESS_EQUAL;
import static jlox.TokenType.MINUS;
import static jlox.TokenType.NIL;
import static jlox.TokenType.NUMBER;
import static jlox.TokenType.OR;
import static jlox.TokenType.PLUS;
import static jlox.TokenType.PRINT;
import static jlox.TokenType.RETURN;
import static jlox.TokenType.RIGHT_BRACE;
import static jlox.TokenType.RIGHT_PAREN;
import static jlox.TokenType.SEMICOLON;
import static jlox.TokenType.SLASH;
import static jlox.TokenType.STAR;
import static jlox.TokenType.STRING;
import static jlox.TokenType.SUPER;
import static jlox.TokenType.THIS;
import static jlox.TokenType.TRUE;
import static jlox.TokenType.VAR;
import static jlox.TokenType.WHILE;

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