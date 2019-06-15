all: compile

compile:
	jflex Scanner.flex
	java -jar java-cup-11b.jar -interface -parser Parser Parser.cup
	javac -cp java-cup-11b-runtime.jar *.java

execute:
	@java -cp java-cup-11b-runtime.jar:. Main

clean:
	rm -f *.class *~
