/*
 *  Copyright 2006 The National Library of New Zealand
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * Created on 27/05/2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nz.govt.natlib.adapter.word;

import java.io.File;
import java.io.IOException;

import nz.govt.natlib.adapter.DataAdapter;
import nz.govt.natlib.adapter.word.OLE.WordOLEAdapter;
import nz.govt.natlib.adapter.word.word2.Word2Adapter;
import nz.govt.natlib.fx.ParserContext;

/**
 * @author nevans
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class WordAdapter extends DataAdapter {

	// knows about two kinds of word file...
	WordOLEAdapter oleAdapter = new WordOLEAdapter();

	Word2Adapter word2Adapter = new Word2Adapter();

	/**
	 * @see nz.govt.natlib.adapter.DataAdapter#acceptsFile(java.io.File)
	 */
	public boolean acceptsFile(File file) {
		return oleAdapter.acceptsFile(file) || word2Adapter.acceptsFile(file);
	}

	public String getName() {
		return "Microsoft Word Adapter";
	}

	public String getDescription() {
		return "Adapts all Microsoft Word files from version 2.0 to XP/2003";
	}

	public String getVersion() {
		return "3.1";
	}

	/**
	 * @see nz.govt.natlib.adapter.DataAdapter#adapt(java.io.File,
	 *      nz.govt.natlib.fx.ParserContext)
	 */
	public void adapt(File file, ParserContext ctx) throws IOException {
		boolean word2 = word2Adapter.acceptsFile(file);
		boolean wordOLE = oleAdapter.acceptsFile(file);
		if (word2 || wordOLE) {
			ctx.fireStartParseEvent("Word");
			writeFileInfo(file, ctx);

			if (word2) {
				ctx.fireParseEvent("Version", "2.0");
				word2Adapter.process(file, ctx);
			}
			// it's an else just in case they both think they can do it or some
			// reason (shouldn't happen)
			else if (wordOLE) {
				ctx.fireParseEvent("Version", "OLE");
				oleAdapter.process(file, ctx);
			}

			ctx.fireEndParseEvent("Word");
		} else {
			throw new RuntimeException("Word Adapter cannot adapt this file "
					+ file);
		}
	}

	public String getOutputType() {
		return "word.dtd";
	}

	public String getInputType() {
		return "application/ms-word";
	}

}
