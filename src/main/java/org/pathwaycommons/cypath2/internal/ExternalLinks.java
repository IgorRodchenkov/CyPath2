// $Id: ExternalLinks.java,v 1.10 2006/06/15 22:06:02 grossb Exp $
//------------------------------------------------------------------------------
/** Copyright (c) 2006 Memorial Sloan-Kettering Cancer Center.
 **
 ** Code written by: Ethan Cerami
 ** Authors: Ethan Cerami, Gary Bader, Chris Sander
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** Memorial Sloan-Kettering Cancer Center
 ** has no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall
 ** Memorial Sloan-Kettering Cancer Center
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** Memorial Sloan-Kettering Cancer Center
 ** has been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/
package org.pathwaycommons.cypath2.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Utility Class for Creating Links to External Databases.
 * 
 * TODO re-write this uring Miriam (MiriamLink) or identifiers.org API to lookup URLs from Xrefs!
 */
final class ExternalLinks {
	
	private static Map<String, String> dbMap;
	private static Map<String, String> ihopMap;
	private static final String SPACE = "%20";
	private static final String PIPE_CHAR = "%7C";
	private static final String AMPERSAND = "&";
	private static final String COMMA = ",";
	private static final String UNIPROT_AC = "UNIPROT__AC";
	private static String pipeChar = PIPE_CHAR;

	/**
	 * Model Object for Encapsulating an External Link.
	 *
	 */
	static final class ExternalLink {
		private String dbName;
		private String id;
		private String relType;
		private String title;
		private String year;
		private String author;
		private String url;
		private String source;
		
		/**
		 * Constructor.
		 *
		 * @param dbName Database name.
		 * @param id     Unique ID.
		 */
		public ExternalLink(String dbName, String id) {
			this.dbName = dbName;
			this.id = id;
		}

		/**
		 * Gets the Database Name.
		 *
		 * @return Database Name.
		 */
		public String getDbName() {
			return dbName;
		}

		/**
		 * Sets the Database Name.
		 *
		 * @param dbName Database Name.
		 */
		public void setDbName(String dbName) {
			this.dbName = dbName;
		}

		/**
		 * Gets the Unique ID.
		 *
		 * @return Unique ID.
		 */
		public String getId() {
			return id;
		}

		/**
		 * Sets the Unique ID.
		 *
		 * @param id Unique ID.
		 */
		public void setId(String id) {
			this.id = id;
		}

		public String getRelType() {
			return relType;
		}

		public void setRelType(String relType) {
			this.relType = relType;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getYear() {
			return year;
		}

		public void setYear(String year) {
			this.year = year;
		}

		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}
	}
	
	private ExternalLinks() {
		throw new AssertionError();
	}
	
	static {
		dbMap = new HashMap<String,String>();
		ihopMap = new HashMap<String,String>();

		//  Pub Med
		String url = "http://www.ncbi.nlm.nih.gov/entrez/"
		             + "query.fcgi?cmd=Retrieve&db=pubmed&dopt=Abstract" + "&list_uids=";
		dbMap.put("PUBMED", url);
		dbMap.put("PMID", url);

        //  HPRD
        url = "http://hprd.org/protein/";
        dbMap.put ("HPRD", url);

        //  UniProt
		url = "http://www.pir.uniprot.org/cgi-bin/upEntry?id=";

		HashMap<String,String> temp = new HashMap<String,String>();
		temp.put("UNIPROT", url);
		temp.put("SWISSPROT", url);
		temp.put("SWP", url);
		temp.put("SWISS-PROT", url);
		dbMap.putAll(temp);
		addIHOPEntries(temp, UNIPROT_AC);

		// Gene Ontology
		url = "http://www.godatabase.org/cgi-bin/amigo/go.cgi?open_1=";
		dbMap.put("GO", url);

		//  Reactome
		url = "http://reactome.org/cgi-bin/eventbrowser?DB=gk_current&ID=";
		dbMap.put("REACTOME DATABASE ID", url);
		url = "http://www.reactome.org/cgi-bin/eventbrowser_st_id?FROM_REACTOME=1&amp;ST_ID=";
		dbMap.put("REACTOME", url);
		dbMap.put("REACTOME STABLE ID", url);

		//  PDB
		url = "http://www.rcsb.org/pdb/cgi/explore.cgi?pdbId=";
		dbMap.put("PDB", url);

		//  Ref Seq
		url = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&" + "cmd=search&term=";
		temp = new HashMap<String,String>();
		temp.put("REFSEQ", url);
		temp.put("REF-SEQ", url);
		temp.put("REF_SEQ", url);
		dbMap.putAll(temp);
		addIHOPEntries(temp, "NCBI_REFSEQ__NP");

		// OMIM
		url = "http://www.ncbi.nlm.nih.gov/entrez/dispomim.cgi?id=";
		dbMap.put("OMIM", url);

		//  Entrez Gene
		url = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&" + "cmd=search&term=";
		temp = new HashMap<String,String>();
        temp.put("ENTREZGENE", url);
        temp.put("ENTREZ_GENE", url);
		temp.put("LOCUS_LINK", url);
		temp.put("LOCUSLINK", url);
		temp.put("LOCUS-LINK", url);
		dbMap.putAll(temp);
		addIHOPEntries(temp, "NCBI_GENE__ID");

		//  Unigene
		url = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?" + "db=unigene&cmd=search&term=";
		dbMap.put("UNIGENE", url);

		//  NCBI GenBank
		url = "http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?db=protein" + "&val=";
		dbMap.put("GENBANK", url);
		dbMap.put("ENTREZ_GI", url);
		dbMap.put("GI", url);
	}
	
	
	/**
	 * Gets a URL to the specified dbName/id Pair.
	 *
	 * @param dbName External Database.
	 * @param id     External ID.
	 * @return a URL String, or null, if dbName is not found.
	 */
	public static String getUrl(String dbName, String id) {
		dbName = dbName.toUpperCase();

		String url = (String) dbMap.get(dbName);

		if (url != null) {
			return url + id;
		} else {
			return null;
		}
	}

	
	/**
	 * Enables URL Encoding.
	 * URL Encoding is on by default.
	 * The only reason to turn this off is to easily view URLs within a unit
	 * test.  Note that URL Encoding must be done in order to properly
	 * launch an External Web Browser.
	 *
	 * @param flag boolean flag.
	 */
	public static void useUrlEncoding(boolean flag) {
		if (flag) {
			pipeChar = "%7C";
		} else {
			pipeChar = "|";
		}
	}

	
	/**
	 * Creates an HTML Link to the specified Database.
	 *
	 * @param dbName External Database.
	 * @param id     External ID.
	 * @return HTML String.
	 */
	public static String createLink(String dbName, String id) {
        dbName = dbName.toUpperCase();
		String url = getUrl(dbName, id);
		StringBuilder buf = new StringBuilder();

        if (url != null) {
			buf.append("<A class=\"link\" HREF=\"" + url + "\">" + dbName + ":  " + id + "</A>");
		} else {
			buf.append(dbName + ":  " + id);
		}

		return buf.toString();
	}

	
	/**
	 * Gets a Link for Searching IHOP.
	 * <p/>
	 * The following rules apply for creating links to IHOP:
	 * <UL>
	 * <LI>Only create links for elements of type:  protein, DNA and RNA.
	 * IHOP does not capture information INFO_ABOUT other BioPAX types.
	 * <LI>If synonyms exist, use them.
	 * <LI>If XRefs for UniProt, Entrez Gene or RefSeq exist, use them.
	 * <LI>If we have at least one synonym or xref, append a taxonomy ID.
	 * <LI>In the special case where we have just one UNIPROT ID, and
	 * no synonyms, we don't have enough information in IHOP to create a
	 * meaningful search result page.  In this case, no link is created.
	 * </UL>
	 * For details on how to construct IHOP links, see:
	 * http://www.pdg.cnb.uam.es/UniPub/iHOP/info/dev/in.html
	 *
	 * @param type       BioPAX Type, e.g. protein
	 * @param synList    ArrayList of Synonym Strings.
	 * @param dbList     ArrayList of ExternalLink Objects.
	 * @param taxonomyId NCBI TaxonomyID or -1 if unknown.
	 * @return URL String, or null if a URL cannot be constructed.
	 */
	public static String getIHOPUrl(String type, List<String> synList, List<ExternalLink> dbList, int taxonomyId) 
	{
		if (type.equalsIgnoreCase("protein") 
			|| type.equalsIgnoreCase("dna")
		    	|| type.equalsIgnoreCase("rna")) 
		{
			StringBuilder url = new StringBuilder();

			// Use the URL Below for local testing within cbio
			// StringBuilder url = new StringBuilder
			//        ("http://cbio.mskcc.org/UniPub/iHOP/in?");
			String synonymParameter = createSynonymParameter(synList);
			String dbParameter = createDbParameter(dbList, synList);

			url.append(synonymParameter);
            if (dbParameter != null && dbParameter.length() > 0) {
                appendAmpersand(synonymParameter, url);
			    url.append(dbParameter);
            }

            //  Taxonomy ID appears like this:
            //  ncbi_tax_id_1=9609
            if (url.length() > 0) {
            	//  removed NCBI Taxonomy ID;  results in nearly always getting a hit w/i iHOP.
            	//	url.append("ncbi_tax_id_1=" + taxonomyId);
            	url.insert(0, "http://www.ihop-net.org/UniPub/iHOP/in?");

            	// return string - but encode spaces first
            	return url.toString().replaceAll("\\s", SPACE);
            }
		} 
		
		return null;
	}

	private static void appendAmpersand(String param, StringBuilder url) {
		if (param.length() > 0) {
			url.append(AMPERSAND);
		}
	}

	
	/**
	 * DBRefs appear like this:
	 * dbrefs_1=UNIPROT__AC|P0214,NCBI_GENE__ID=327..
	 */
	private static String createDbParameter(List<ExternalLink> dbList, List<String> synList) {
		int dbHits = 0;
		int uniProtHits = 0;
		StringBuilder temp = new StringBuilder();

		if ((dbList != null) && (dbList.size() > 0)) {
			for (int i = 0; i < dbList.size(); i++) {
				ExternalLink link = (ExternalLink) dbList.get(i);
				if(link != null && link.getDbName() != null) {
					String code = (String) ihopMap.get(link.getDbName().toUpperCase());
					if (code != null) {
						if (code.equals(UNIPROT_AC)) {
							uniProtHits++;
						}

						dbHits++;
						temp.append(code + pipeChar + link.getId() + COMMA);
					}
				}
			}

			if (temp.length() > 0) {
				//  This is a special case.
				if ((uniProtHits == dbHits) && (synList.size() == 0)) {
					return new String();
				} else {
					//  Insert parameter name; remove last comma
					temp.insert(0, "dbrefs_1=");

					return temp.substring(0, temp.length() - 1);
				}
			}
		}

		return new String();
	}

	
	/**
	 * Synonyms appear like this:
	 * syns_1=SYN1|SYN2|SYN3...
	 */
	private static String createSynonymParameter(List<String> synList) {
		StringBuilder temp = new StringBuilder();

		if ((synList != null) && (synList.size() > 0)) {
			temp.append("syns_1=");

			for (int i = 0; i < synList.size(); i++) {
				temp.append(synList.get(i));

				if (i < (synList.size() - 1)) {
					temp.append(pipeChar);
				}
			}
		}

		return temp.toString();
	}

	
	/**
	 * Creates HTML for a Link to IHOP.
	 *
	 * @param type       BioPAX Type, e.g. protein
	 * @param synList    ArrayList of Synonym Strings.
	 * @param linkList   ArrayList of ExternalLink Objects.
	 * @param taxonomyId NCBI TaxonomyID or -1 if unknown.
	 * @return HTML Link.
	 */
	public static String createIHOPLink(String type, 
			List<String> synList, List<ExternalLink> linkList, int taxonomyId) 
	{
		String url = getIHOPUrl(type, synList, linkList, taxonomyId);

        if (url != null) {
			StringBuilder buf = new StringBuilder();
			buf.append("<A class=\"link\" HREF=\"" + url + "\">" + "Search iHOP</A>");
			return buf.toString();
		} else {
			return null;
		}
	}


	private static void addIHOPEntries(Map<String,String> map, String iHopCode) {
		Iterator<String> iterator = map.keySet().iterator();

		while (iterator.hasNext()) {
			String key = iterator.next();
			ihopMap.put(key, iHopCode);
		}
	}
}