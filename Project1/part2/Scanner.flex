import java_cup.runtime.*;

%%
%class Scanner
%line
%column
%cup
%unicode

%{
    StringBuffer stringBuffer = new StringBuffer();
    private Symbol symbol(int type) {
       return new Symbol(type, yyline, yycolumn);
    }
    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline, yycolumn, value);
    }
%}

letter          = [A-Za-z]
digit           = [0-9]
alphanumeric    = {letter}|{digit}
other_id_char   = [_]
LineTerminator = \r|\n|\r\n
WhiteSpace     = {LineTerminator} | [ \t\f]
Identifier 	   = {letter}({alphanumeric}|{other_id_char})*

%state STRING

%%
<YYINITIAL> {
 if           	{ return symbol(sym.IF); }
 else         	{ return symbol(sym.ELSE); }
 prefix       	{ return symbol(sym.PREFIX); }
 suffix       	{ return symbol(sym.SUFFIX); }
 {Identifier}   { return symbol(sym.IDENTIFIER, yytext()); }
 "+"            { return symbol(sym.PLUS); }
 "("            { return symbol(sym.LPAREN); }
 ")"            { return symbol(sym.RPAREN); }
 ","       		{ return symbol(sym.COMMA); }
 "{"       		{ return symbol(sym.LBRACKET); }
 "}"       		{ return symbol(sym.RBRACKET); }
 "\""           { stringBuffer.setLength(0); yybegin(STRING); }
 {WhiteSpace}   { /* just skip what was found, do nothing */ }
}

<STRING> {
      \"                { yybegin(YYINITIAL);
                            return symbol(sym.STRING_LITERAL, stringBuffer.toString()); }
      [^\n\r\"\\]+      { stringBuffer.append( yytext() ); }
      \\t               { stringBuffer.append('\t'); }
      \\n               { stringBuffer.append('\n'); }

      \\r               { stringBuffer.append('\r'); }
      \\\"              { stringBuffer.append('\"'); }
      \\                { stringBuffer.append('\\'); }
}

[^]           { throw new Error("Illegal character <"+yytext()+">"); }
