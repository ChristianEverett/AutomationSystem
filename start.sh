#!/bin/bash
if [ "$#" -ne 1 ]; then
    sudo java -jar AutomationController.jar &
else
	sudo java -client -agentlib:jdwp=transport=dt_socket,server=y,address=1044 -jar AutomationController.jar &
fi