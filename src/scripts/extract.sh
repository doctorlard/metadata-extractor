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
$JAVA_HOME/bin/java -Xmx128m -Dmetahome=$METAHOME -Djava.system.class.loader=nz.govt.natlib.meta.config.Loader -cp $METAHOME/lib/metadata.jar:$METAHOME/lib/xalan.jar:$METAHOME/lib/xercesImpl.jar:$METAHOME/lib/xml-apis.jar:$METAHOME/lib/serializer.jar:$METAHOME/lib/poi-2.5.1-final-20040804.jar:$METAHOME/lib/bfj220.jar:$METAHOME/lib/PDFBox-0.7.3.jar:$METAHOME/lib/bcprov-jdk14-132.jar:$METAHOME/lib/bcmail-jdk14-132.jar:$METAHOME/lib/jid3lib-0.5.4.jar:$METAHOME/lib/heritrix-1.14.1.jar:$METAHOME/lib/fastutil-5.0.3-heritrix-subset-1.0.jar:$METAHOME/lib/commons-logging-1.1.jar:$METAHOME/lib/commons-httpclient-3.1.jar:$METAHOME nz.govt.natlib.meta.ui.CmdLine $*