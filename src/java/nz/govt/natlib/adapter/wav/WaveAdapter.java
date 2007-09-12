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

package nz.govt.natlib.adapter.wav;

import java.io.File;
import java.io.IOException;

import nz.govt.natlib.adapter.AdapterUtils;
import nz.govt.natlib.adapter.DataAdapter;
import nz.govt.natlib.fx.CompoundElement;
import nz.govt.natlib.fx.ConstantElement;
import nz.govt.natlib.fx.DataSource;
import nz.govt.natlib.fx.Element;
import nz.govt.natlib.fx.FXUtil;
import nz.govt.natlib.fx.FileDataSource;
import nz.govt.natlib.fx.FixedLengthStringElement;
import nz.govt.natlib.fx.IntegerElement;
import nz.govt.natlib.fx.ParserContext;
import nz.govt.natlib.fx.PositionalElement;
import nz.govt.natlib.fx.StringElement;

/**
 * WaveAdapter is responsible for extracting metadata out of Audio Wave files.
 * 
 * @author Nic Evans
 * @version 1.0
 */
public class WaveAdapter extends DataAdapter {

	/** All WAV files should start with this hex header */
	public static final String WAV_HEADER = "52 49 46 46 xx xx xx xx 57 41 56 45 66 6D 74 20";

	private Element riffElement = new CompoundElement(new String[] { "Length",
			"SubType" }, new Element[] {
			new IntegerElement(IntegerElement.INT_SIZE, false,
					IntegerElement.DECIMAL_FORMAT),
			new FixedLengthStringElement(4), });

	// the compound element "parser" to read a wave header
	private Element wavElement = new CompoundElement(new String[] { "Length",
			"Format", "Channels", "SamplesPerSec", "AverageBytesPerSec",
			"nBlockAlign", "BitsPerSample" }, new Element[] {
			new IntegerElement(IntegerElement.INT_SIZE, false,
					IntegerElement.DECIMAL_FORMAT),
			new IntegerElement(IntegerElement.SHORT_SIZE, false,
					IntegerElement.DECIMAL_FORMAT),
			new IntegerElement(IntegerElement.SHORT_SIZE, false,
					IntegerElement.DECIMAL_FORMAT),
			new IntegerElement(IntegerElement.INT_SIZE, false,
					IntegerElement.DECIMAL_FORMAT),
			new IntegerElement(IntegerElement.INT_SIZE, false,
					IntegerElement.DECIMAL_FORMAT),
			new IntegerElement(IntegerElement.SHORT_SIZE, false,
					IntegerElement.DECIMAL_FORMAT),
			new IntegerElement(IntegerElement.SHORT_SIZE, false,
					IntegerElement.DECIMAL_FORMAT), });

	// the compound element "parser" to read a wave header
	private String[] bextNames = new String[] { "Length", "Description",
			"Originator", "OriginatorRef", "OriginatorDate",
			"OriginatorDateFormat", "OriginatorTime", "OriginatorTimeFormat",
			"TimeReferenceLow", "TimeReferenceHi", "Version", "UMID",
			"Reserved", "CodingHistory" };

	Element hidden64 = new PositionalElement(64); // UUID

	Element hidden190 = new PositionalElement(190); // Reserved

	private Element bextElement = new CompoundElement(bextNames, new Element[] {
			new IntegerElement(IntegerElement.INT_SIZE, false,
					IntegerElement.DECIMAL_FORMAT),
			new FixedLengthStringElement(256),
			new FixedLengthStringElement(32),
			new FixedLengthStringElement(32),
			new FixedLengthStringElement(10),
			new ConstantElement("'yyyy-mm-dd'"),
			new FixedLengthStringElement(8),
			new ConstantElement("'hh:mm:ss'"),
			new IntegerElement(IntegerElement.INT_SIZE, false,
					IntegerElement.DECIMAL_FORMAT),
			new IntegerElement(IntegerElement.INT_SIZE, false,
					IntegerElement.DECIMAL_FORMAT),
			new IntegerElement(IntegerElement.INT_SIZE, false,
					IntegerElement.DECIMAL_FORMAT), hidden64, // UUID
			hidden190, // Reserved
			new StringElement(), });

	// the compound element "parser" to read a wave data
	private Element genericElement = new CompoundElement(
			new String[] { "Length" }, new Element[] { new IntegerElement(
					IntegerElement.INT_SIZE, false,
					IntegerElement.DECIMAL_FORMAT), });

	public WaveAdapter() {
		hidden64.setInternal(true);
		hidden190.setInternal(true);
	}

	public boolean acceptsFile(File file) {
		return checkFileHeader(file, WAV_HEADER);
	}

	public String getOutputType() {
		return "wav.dtd";
	}

	public String getInputType() {
		return "audio/wav";
	}

	public String getName() {
		return "Microsoft WAV Audio Adapter";
	}

	public String getDescription() {
		return "Adapts all Microsoft WAV audio files, includes BWF";
	}

	public String getVersion() {
		return "2.0";
	}

	public void adapt(File file, ParserContext ctx) throws IOException {
		// add the MetaData to the tree!
		DataSource ftk = null;
		
		try {
			ftk = new FileDataSource(file);
			
			ctx.fireStartParseEvent("WAV");
			writeFileInfo(file, ctx);
	
			ctx.fireStartParseEvent("RIFF");
	
			// move 4 bytes in (for the RIFF tag)
			ftk.setPosition(4);
			riffElement.read(ftk, ctx);
	
			ctx.fireEndParseEvent("RIFF");
	
			// 1. is there a chunk to be read...
			long chunkSize = 12;
			long riffHeaderLength = ctx.getIntAttribute("WAV.RIFF.length");
			while (chunkSize < riffHeaderLength) {
				// 2. read the chunk...
				// get the file position...
				long startPos = ftk.getPosition();
				long reportedLength = 0;
				// read the type;
				String st = FXUtil.getFixedStringValue(ftk, 4);
	
				// 3. process the chunk...
				Element parser = null;
				// get data for the type of block it is...
				if ("fmt ".equals(st)) {
					ctx.fireStartParseEvent("Wave");
					ctx.fireParseEvent("type", st);
					wavElement.read(ftk, ctx);
					ctx.fireEndParseEvent("Wave");
					reportedLength = ctx.getIntAttribute("WAV.Wave.length");
					// break; // only want the first format tag...
				} else if ("data".equals(st)) {
					ctx.fireStartParseEvent("data");
					ctx.fireParseEvent("type", st);
					genericElement.read(ftk, ctx);
					ctx.fireEndParseEvent("data");
					reportedLength = ctx.getIntAttribute("WAV.data.length");
				} else if ("bext".equals(st)) {
					// System.out.println("Broadcast Wave Format extension chunk
					// found");
					ctx.fireStartParseEvent("bext");
					ctx.fireParseEvent("type", st);
					bextElement.read(ftk, ctx);
					ctx.fireEndParseEvent("bext");
					reportedLength = ctx.getIntAttribute("WAV.bext.length");
	
					// debug the bext
					// System.out.println("BEXT ->");
					// for (int i=0;i<bextNames.length;i++) {
					// System.out.println(bextNames[i]+"="+ctx.getAttribute("WAV.bext."+bextNames[i]));
					// }
				} else {
					// System.out.println("Unknown RIFF chunk :"+st);
					ctx.fireStartParseEvent("unknown");
					ctx.fireParseEvent("type", st);
					genericElement.read(ftk, ctx);
					ctx.fireEndParseEvent("unknown");
					reportedLength = ctx.getIntAttribute("WAV.unknown.length");
				}
	
				// work out if there is any padding at the end of the chunk
				long endPos = ftk.getPosition();
				long moveTo = startPos + reportedLength + 8; // the 8's 'cause
				// there's a 2 word
				// format/length
				// added to the
				// chunk at the
				// start
	
				// repositioning
				// System.out.println("Repo :");
				// System.out.println(" Reported Length :"+reportedLength);
				// System.out.println(" End :"+endPos);
				// System.out.println(" Start :"+startPos);
				// System.out.println(" Move Formula :"+moveTo);
	
				// 4. move the file pointer to the next chunk...
				ftk.setPosition(moveTo);
				// 5. loop...
				chunkSize += (reportedLength + 8);
			}
			ctx.fireEndParseEvent("WAV");
		}
		finally {
			AdapterUtils.close(ftk);
		}
	}

	private static String getTime(long ms) {
		// min:sec.ms
		long min = ms / 60000;
		long sec = (ms - (min * 60000)) / 1000;
		long milli = (ms - (min * 60000) - (sec * 1000));
		return min + ":" + sec + "." + milli;
	}

}