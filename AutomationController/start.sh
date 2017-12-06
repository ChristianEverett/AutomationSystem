#!/bin/bash
if [ "$#" -ne 1 ]; then
    sudo java -client -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044 -jar AutomationController.jar &
else
	sudo java -client -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1044 -jar AutomationController.jar &
fi
#jmap -dump:format=b,file=cheap.bin <pid>
#echo 'int main() { printf("%d", sizeof(void)); }' | gcc -xc -w - && ./a.out 