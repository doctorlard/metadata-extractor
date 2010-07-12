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

package nz.govt.natlib.adapter.warc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import nz.govt.natlib.adapter.DataAdapter;
import nz.govt.natlib.fx.ParserContext;
import nz.govt.natlib.meta.log.LogManager;
import nz.govt.natlib.meta.log.LogMessage;

import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;

/**
 * WarcAdapter is responsible for extracting metadata out of Internet Archive "WARC" files.
 * 
 * <p>For official specification of WARC files, see the URLs:
 * <br>http://www.iso.org/iso/iso_catalogue/catalogue_tc/catalogue_detail.htm?csnumber=44717
 * <br>http://bibnum.bnf.fr/WARC/warc_ISO_DIS_28500.pdf
 * 
 * <p>Additional resources for WARC format can be found at:
 * <br>http://www.digitalpreservation.gov/formats/fdd/fdd000236.shtml
 * <br>http://archive-access.sourceforge.net/warc/
 * <br>http://archive-access.sourceforge.net/warc/warc_file_format-0.9.html
 * 
 * <p>This class makes a heavy use of the Heritrix library available from Internet Archive website at:
 * <br>http://crawler.archive.org/
 * 
 * <p><b>Note:</b>
 * <br>Implementation of this class is not yet complete.
 * <br>For example, this class doesn't seem to work properly for uncompressed WARC files. The Heritrix library
 * throws IOException while iterating through the WARC records in the adapt() method. It could be either
 * due to a bug in Heritrix or due to the non-conformity of the uncompressed WARC files that have been used
 * to test this adapter. Need to investigate more. For all compressed WARC (warc.gz) files, this class seems
 * to work fine.
 * <br>Also, need to investigate what other metadata needs to be collected from a WARC file.
 * 
 * @author Raghu Pushpakath
 * @version 1.0
 */
public class WarcAdapter extends DataAdapter {

	private static WARCReaderFactory factory = getARCReaderFactory();

	private static final String SOFTWARE = "software";
	private static final String HOSTNAME = "hostname";
	private static final String IP = "ip";
	private static final String OPERATOR = "operator";
	private static final String CREATEDDATE = "created";
	private static final String ROBOTPOLICY = "robots";
	private static final String WARCFORMAT = "format";
	private static final String CONFORMSTO = "conformsTo";

	private static final Set WARC_TYPES_OF_INTEREST = new TreeSet(String.CASE_INSENSITIVE_ORDER);

	private static final int MAX_LINES = 100;

	private class WarcMetadata {
		private String software;
		private String hostname;
		private String ip;
		private String operator;
		private String createdDate;
		private String robotPolicy;
		private String warcFormat;
		private String conformsTo;
	}

	static {
		WARC_TYPES_OF_INTEREST.add("response");
		WARC_TYPES_OF_INTEREST.add("resource");
		WARC_TYPES_OF_INTEREST.add("continuation");
		WARC_TYPES_OF_INTEREST.add("revisit");
	}

	public WarcAdapter() {
	}

	public boolean acceptsFile(File file) {
		boolean arc = false;
		WARCReader warcReader = null;
		try {
			warcReader = getWARCReader(file);
			arc = (warcReader != null);
			return arc;
		} catch (Exception ex) {
			LogManager.getInstance().logMessage(LogMessage.WORTHLESS_CHATTER,
					"IO Exception determining WARC file type");
		} finally {
			if (warcReader != null)
				try {
					warcReader.close();
				} catch (IOException e) {
				}
		}
		return arc;
	}

	public String getOutputType() {
		return "warc.dtd";
	}

	public String getInputType() {
		return "application/warc";
	}

	public String getName() {
		return "Internet Archive WARC File Adapter";
	}

	public String getDescription() {
		return "Adapts Internet archive WARC files (both compressed and uncompressed)";
	}

	public String getVersion() {
		return "1.0";
	}

	public void adapt(File file, ParserContext ctx) throws IOException {
		WARCReader warcReader = null;
		try {
			// Get the reader (either compressed or uncompressed)
			warcReader = getWARCReader(file);
			// Get an iterator over the warc records
			Iterator iter = warcReader.iterator();
			// Reference to the first record which is the "archive metadata record"
			WarcMetadata arcMetadata = null;
			// Map to hold the mime type statistics
			HashMap mimeMap = new HashMap();
			HashMap warcTypeMap = new HashMap();
			// Iterate over the warc records
			try {
				while (iter != null && iter.hasNext()) {
					/*
					 * TODO:
					 * The method call below seems to throw IOException
					 * for uncompressed WARC files. It works fine for
					 * compressed WARC (warc.gz) files. It could be either
					 * due to a bug in Heritrix or due to the non-conformity 
					 * of the uncompressed WARC files that have been used
					 * to test this adapter.
					 */
					ArchiveRecord record = (ArchiveRecord)iter.next();

					/*
					 * Get required information from the retrieved WARC record
					 */
					String warcType = getWarcType(record);
					addContentToMap(warcType, warcTypeMap);
					/*
					 * Extract the warc metadata from the warcinfo record.
					 * Extract the mime type info from "response" and other
					 * specific types of warc records.
					 */
					if ("warcinfo".equals(warcType) && arcMetadata == null) {
						// Extract the metadata from this warc record
						arcMetadata = parseArcMetadataRecord(record);
					} else if (WARC_TYPES_OF_INTEREST.contains(warcType)) {
						// Add logic later if we ever need to get metadata out of other
						// WARC types.
					}
					addMimeTypeToMimeMap(record, mimeMap);
					record.close();
				}
			} catch (Exception ex) {
				System.out.println("Exception while iterating through WARC records: " + ex);
				ex.printStackTrace();
			}

			ctx.fireStartParseEvent("WARC");
			writeFileInfo(file, ctx);

			// Write the <ARCMETADATA> element
			if (arcMetadata != null) {
				ctx.fireStartParseEvent("WARCMETADATA");
				ctx.fireParseEvent("SOFTWARE", arcMetadata.software);
				ctx.fireParseEvent("HOSTNAME", arcMetadata.hostname);
				ctx.fireParseEvent("IP", arcMetadata.ip);
				ctx.fireParseEvent("OPERATOR", arcMetadata.operator);
				ctx.fireParseEvent("CREATEDDATE", arcMetadata.createdDate);
				ctx.fireParseEvent("ROBOTPOLICY", arcMetadata.robotPolicy);
				ctx.fireParseEvent("WARCFORMAT", arcMetadata.warcFormat);
				ctx.fireParseEvent("CONFORMSTO", arcMetadata.conformsTo);
				ctx.fireEndParseEvent("WARCMETADATA");
			}

			// Write the <ARCINFO> element
			ctx.fireStartParseEvent("WARCINFO");
			ctx.fireParseEvent("COMPRESSED", warcReader.isCompressed());

			ctx.fireStartParseEvent("CONTENTSUMMARY");

			if (warcTypeMap.size() > 0) {
				Set keys = new TreeSet(String.CASE_INSENSITIVE_ORDER);
				keys.addAll(warcTypeMap.keySet()); 
				iter = keys.iterator();
				while (iter != null && iter.hasNext()) {
					String warctype = (String)iter.next();
					ctx.fireStartParseEvent("WARCTYPEREPORT");
					ctx.fireParseEvent("WARCTYPE", warctype);
					ctx.fireParseEvent("COUNT", warcTypeMap.get(warctype));
					ctx.fireEndParseEvent("WARCTYPEREPORT");
				}
			}
			if (mimeMap.size() > 0) {
				Set keys = new TreeSet(String.CASE_INSENSITIVE_ORDER);
				keys.addAll(mimeMap.keySet()); 
				iter = keys.iterator();
				while (iter != null && iter.hasNext()) {
					String mimetype = (String)iter.next();
					ctx.fireStartParseEvent("MIMEREPORT");
					ctx.fireParseEvent("MIMETYPE", mimetype);
					ctx.fireParseEvent("COUNT", mimeMap.get(mimetype));
					ctx.fireEndParseEvent("MIMEREPORT");
				}
			}
			ctx.fireEndParseEvent("CONTENTSUMMARY");
			ctx.fireEndParseEvent("WARCINFO");
			ctx.fireEndParseEvent("WARC");
		} catch (Throwable ex) {
			System.out.println("Exception: " + ex);
			ex.printStackTrace();
		} finally {
			if (warcReader != null)
				warcReader.close();
		}
	}

	private WarcMetadata parseArcMetadataRecord(ArchiveRecord arcMetadataRecord) {
		if (arcMetadataRecord == null) return null;
		WarcMetadata metadata = new WarcMetadata();
		ByteArrayOutputStream bos = null;
		try {
			bos = new ByteArrayOutputStream();
			arcMetadataRecord.dump(bos);
			byte[] data = bos.toByteArray();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
			String line = "";
			while ((line = reader.readLine()) != null) {
				metadata.software = getIfMatches(line, SOFTWARE, metadata.software);
				metadata.hostname = getIfMatches(line, HOSTNAME, metadata.hostname);
				metadata.ip = getIfMatches(line, IP, metadata.ip);
				metadata.operator = getIfMatches(line, OPERATOR, metadata.operator);
				metadata.createdDate = getIfMatches(line, CREATEDDATE, metadata.createdDate);
				metadata.robotPolicy = getIfMatches(line, ROBOTPOLICY, metadata.robotPolicy);
				metadata.warcFormat = getIfMatches(line, WARCFORMAT, metadata.warcFormat);
				metadata.conformsTo = getIfMatches(line, CONFORMSTO, metadata.conformsTo);
			}
			return metadata;
		} catch (Exception e) {
		} finally {
			if (bos != null){ 
				try {
					bos.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

	private String getIfMatches(String line, String lineToLookFor, String oldValue) {
		if (oldValue != null) return oldValue;
		String token = lineToLookFor + ": ";
		if (line.startsWith(token)) {
			return line.replaceFirst(token, "");
		}
		return null;
	}

	private void addMimeTypeToMimeMap(ArchiveRecord record, HashMap mimeMap) {
		/*
		 * TODO: Need to determine what kind of WARC records can have Content-Type.
		 * Is it only for the "response" record type? Not sure.
		 * Investigate this and skip this method for all other record types.
		 */
		String mime = null;
		List lines = new ArrayList();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(record));
			String line = "";
			int count = 0;
			String contentTypeString = "Content-Type: ";
			while ((line = reader.readLine()) != null && count < MAX_LINES && mime == null) {
				//System.out.println("Line: " + line);
				lines.add(line);
				if (line.startsWith(contentTypeString)) {
					mime = line.replaceFirst(contentTypeString, "");
				}
				count++;
			}
		} catch (Exception e) {
		} finally {
		}
		if (mime == null) {
			mime = "not recorded";
		}
		addContentToMap(mime, mimeMap);
	}

	private String getWarcType(ArchiveRecord warcRecord) {
		ArchiveRecordHeader header = warcRecord.getHeader();
		if (header != null)
			return (String)header.getHeaderValue("WARC-Type");
		return null;
	}

	private void addContentToMap(String content, HashMap contentMap) {
		if (content == null) return;
		content = content.trim();
		Object counterObj = contentMap.get(content);
		int count = 0;
		if (counterObj != null) {
			count = ((Integer)counterObj).intValue();
		}
		count++;
		contentMap.put(content, new Integer(count));
	}

	private WARCReader getWARCReader(File file) {
		try {
			return getWARCReader(file, false);
		} catch (Exception ex) {
			try {
				return getWARCReader(file, true);
			} catch (Exception e) {
				return null;
			}
		}
	}

	private WARCReader getWARCReader(File file, boolean compressed) {
		WARCReader warcReader = null;
		boolean isError = false;
		try {
			if (factory == null) {
				isError = true;
				throw new RuntimeException("Could not get WARCReaderFactory instance through reflection");
			}
			if (compressed)
				warcReader = factory. new CompressedWARCReader(file);
			else
				warcReader = factory. new UncompressedWARCReader(file);
			if (warcReader.isValid()) {
				return warcReader;
			} else {
				isError = true;
				throw new RuntimeException("WARCReader is invalid");
			}
		} catch (Exception ex) {
			isError = true;
			throw new RuntimeException("WARCReader is invalid", ex);
		} finally {
			if (isError == true) {
				try {
					if (warcReader != null)
						warcReader.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static WARCReaderFactory getARCReaderFactory() {
		try {
			Field privateStringField = WARCReaderFactory.class.getDeclaredField("factory");
			privateStringField.setAccessible(true);
			WARCReaderFactory factory = (WARCReaderFactory) privateStringField.get(null);
			return factory;
		} catch (Exception ex) {
			throw new RuntimeException("Could not get WARCReaderFactory instance through reflection", ex);
		}
	}
}