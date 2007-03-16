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

package nz.govt.natlib.adapter.powerpoint;

import nz.govt.natlib.fx.CompoundElement;
import nz.govt.natlib.fx.DataSource;
import nz.govt.natlib.fx.Element;
import nz.govt.natlib.fx.IntegerElement;
import nz.govt.natlib.fx.ParserContext;

/**
 * 
 * @author Nic Evans
 * @version 1.0
 */

public class OLEUtils {
	// File position offset to the NFib
	private static final int Offset_To_NFib = 0x2;

	public static void getHeader(DataSource ftk, ParserContext ctx)
			throws java.io.IOException {
		Element wordHeader = new CompoundElement(new String[] { "MagicNumber",
				"FIBNumber", "ProductVersion" }, new Element[] {
				new IntegerElement(IntegerElement.SHORT_SIZE, false,
						IntegerElement.HEX_FORMAT),
				new IntegerElement(IntegerElement.SHORT_SIZE, false,
						IntegerElement.HEX_FORMAT),
				new IntegerElement(IntegerElement.SHORT_SIZE, false,
						IntegerElement.HEX_FORMAT), });

		wordHeader.read(ftk, ctx);
	}

	public static void getDOPProperties(DataSource ftk, int iDopFP,
			ParserContext ctx) throws java.io.IOException {
		// Set to the Dop + offset to start of relevant information
		ftk.setPosition((iDopFP + 0x14));
		Element DocumentProperties = new CompoundElement(new String[] {
				"created", "revision", "printed", "revisioncount",
				"timeediting", "pages", "words", "characters" }, new Element[] {
				new DTTM(),
				new DTTM(),
				new DTTM(),
				new IntegerElement(IntegerElement.SHORT_SIZE, false,
						IntegerElement.DECIMAL_FORMAT),
				new IntegerElement(IntegerElement.INT_SIZE, false,
						IntegerElement.DECIMAL_FORMAT),
				new IntegerElement(IntegerElement.INT_SIZE, false,
						IntegerElement.DECIMAL_FORMAT),
				new IntegerElement(IntegerElement.INT_SIZE, false,
						IntegerElement.DECIMAL_FORMAT),
				new IntegerElement(IntegerElement.SHORT_SIZE, false,
						IntegerElement.DECIMAL_FORMAT), });
		DocumentProperties.read(ftk, ctx);
	}
}