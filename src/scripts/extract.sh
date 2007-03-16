#!/bin/sh
############################################################################
# Startup Script for the Metadata Extraction Tool v3.0 - Command Line Tool
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
$JAVA_HOME/bin/java -cp ./lib/metadata.jar:./lib/xalan.jar:./lib/xercesImpl.jar:./lib/xml-apis.jar:./lib/serializer.jar:./lib/poi-2.5.1-final-20040804.jar:./lib/bfj220.jar:. nz.govt.natlib.meta.ui.CmdLine $*