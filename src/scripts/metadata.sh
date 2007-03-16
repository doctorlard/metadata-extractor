#!/bin/sh
############################################################################
# Startup Script for the Metadata Extraction Tool v3.0
############################################################################

# If the METAHOME directory is not yet set, try to guess it.
if [ -z "$METAHOME" ] ; then
  METAHOME=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
fi

# A check to make sure we guessed okay.
if [ ! -f "$METAHOME/config.xml" ] ; then
  echo Failed to guess home directory.
  exit
fi



# Start the tool.
$JAVA_HOME/bin/java -Dmetahome=$METAHOME -Djava.system.class.loader=nz.govt.natlib.meta.config.Loader -cp $METAHOME/lib/metadata.jar:$METAHOME/lib/xalan.jar:$METAHOME/lib/xercesImpl.jar:$METAHOME/lib/xml-apis.jar:$METAHOME/lib/serializer.jar:$METAHOME/lib/poi-2.5.1-final-20040804.jar:$METAHOME/lib/bfj220.jar:$METAHOME nz.govt.natlib.meta.ui.Main
