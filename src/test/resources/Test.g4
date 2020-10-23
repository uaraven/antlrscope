grammar Test;

start:
  twoWords
  EOF
  ;

twoWords: WORD SPACE WORD;

WORD  : [a-zA-Z0-9_]+  ;
SPACE : ' ' | '\t' ;