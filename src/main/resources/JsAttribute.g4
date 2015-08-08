grammar JsAttribute;
r  : (ATTR | ARRATTR)+ ;   

ARRATTR: '[' [0-9]+ ']';
ATTR: '.' [-a-zA-Z0-9_]+;

//字符串难，就不写了
WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines