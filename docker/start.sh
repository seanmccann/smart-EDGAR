#!/bin/sh
# Start smart edgar
echo java -Xmx$xmx -server -Dlog4j.configuration=file:log4j.properties -cp *.jar $1 $2 $3 
java -Xmx$xmx -server -Dlog4j.configuration=file:log4j.properties -cp *.jar $1 $2 $3 
