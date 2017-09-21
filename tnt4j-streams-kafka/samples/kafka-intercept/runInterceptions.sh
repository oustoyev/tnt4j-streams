#! /bin/bash
if command -v realpath >/dev/null 2>&1; then
    SCRIPTPATH=`dirname $(realpath $0)`
else
    SCRIPTPATH=$( cd "$(dirname "$0")" ; pwd -P )
fi

LIBPATH="$SCRIPTPATH/../../*:$SCRIPTPATH/../../lib/*"
TNT4JOPTS="-Dtnt4j.config=$SCRIPTPATH/../../config/intercept/tnt4j_kafka.properties"
LOG4JOPTS="-Dlog4j.configuration=file:$SCRIPTPATH/../../config/log4j.properties"
PRODUCER_CONFIG="-Dproducer.config=$SCRIPTPATH/../../config/intercept/producer.properties"
CONSUMER_CONFIG="-Dconsumer.config=$SCRIPTPATH/../../config/intercept/consumer.properties"
INTERCEPT_CONFIG="-Dinterceptors.config=$SCRIPTPATH/../../config/intercept/interceptors.properties"
STREAMSOPTS="$LOG4JOPTS $TNT4JOPTS $PRODUCER_CONFIG $CONSUMER_CONFIG $INTERCEPT_CONFIG"

java $STREAMSOPTS -classpath "$LIBPATH" com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.InterceptorsTest

read -p "Press [Enter] key to exit..."