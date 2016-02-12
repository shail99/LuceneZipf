package edu.neu.cs6200.luceneSimpleAnalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;

/**
 * To create Apache Lucene index in a folder and add files into this index based
 * on the input of the user.
 */
public class HW4 {
    //private static Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
    private static Analyzer sAnalyzer = new SimpleAnalyzer(Version.LUCENE_47);
    private static LinkedHashMap<String,Long> unique_term = new LinkedHashMap<String,Long>();

    private IndexWriter writer;
    private ArrayList<File> queue = new ArrayList<File>();

    public static void main(String[] args) throws IOException {
	System.out
		.println("Enter the FULL path where the index will be created: (e.g. /Usr/index or c:\\temp\\index)");

	String indexLocation = null;
	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	String s = br.readLine();

	HW4 indexer = null;
	try {
	    indexLocation = s;
	    indexer = new HW4(s);
	} catch (Exception ex) {
	    System.out.println("Cannot create index..." + ex.getMessage());
	    System.exit(-1);
	}

	// ===================================================
	// read input from user until he enters q for quit
	// ===================================================
	while (!s.equalsIgnoreCase("q")) {
	    try {
		System.out.println("Enter the FULL path to add into the index (q=quit): (e.g. /home/mydir/docs or c:\\Users\\mydir\\docs)");
		System.out
			.println("[Acceptable file types: .xml, .html, .html, .txt]");
		s = br.readLine();
		if (s.equalsIgnoreCase("q")) {
		    break;
		}

		// try to add file into the index
		indexer.indexFileOrDirectory(s);
	    } catch (Exception e) {
		System.out.println("Error indexing " + s + " : "
			+ e.getMessage());
	    }
	}

	// ===================================================
	// after adding, we always have to call the
	// closeIndex, otherwise the index is not created
	// ===================================================
	indexer.closeIndex();

	IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(
		indexLocation)));
	
	// =========================================================
	// Get the pairs of unique_term and frequency over the corpus
	// ==========================================================
		indexer.getListPairs(reader);
		
	// ===================================================	
	// sort the entire list based on the frequency
	// ===================================================
		indexer.sort();
		
	// =============================================================
	// write the sorted list by frequency of (term, term_freq pairs)
	// =============================================================
		indexer.writeToFile();
		
	// =============================================================		
	// plot the chart i.e. (Zipf's law)
	// =============================================================
		new Plot(unique_term);
				
	// =========================================================
	// Now search for each individual queries in queries.txt
	// within the index created by the IndexWriter
	// =========================================================

	s = "";
	
	BufferedReader input = new BufferedReader(new FileReader("queries.txt"));
	BufferedWriter output = new BufferedWriter(new FileWriter("results.txt"));
	
	while ((s = input.readLine()) != null) {
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(30000, true);
	    try {
		
		Query q = new QueryParser(Version.LUCENE_47, "contents",sAnalyzer).parse(s);
		
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		// 1. Writing the results into a file "results.txt"
		// the file containing the top 100 document list sorted by rank
		System.out.println("Found " + hits.length + " hits for "+ s);
		String txtLine = "Results for query: "+s+",No of Hits: "+hits.length+"\n";
		int count = 1;
		for (int i = 0; i < hits.length; ++i) {
		    int docId = hits[i].doc;
		    Document d = searcher.doc(docId);
		    txtLine += (i + 1) +" "+  d.get("path").split("\\/")[1]+ " score=" + hits[i].score +" "+getCharSnippet(d.get("contents"))+"\n";
		    
		    if(count == 100)
		    	break;
		    
		    count++;
		}
		txtLine+="\n";
		
		output.write(txtLine);
		// 2. term stats --> watch out for which "version" of the term
		// must be checked here instead!
		//Term termInstance = new Term("contents", s);
	    } catch (Exception e) {
		System.out.println("Error searching " + s + " : " + e.getMessage());
		e.printStackTrace();
		break;
	    }
	}
	
	// close the input and output files after reading and writing respectively
	input.close();
	output.close();

    }

    // =======================================================================
    // getCharSnippet function to get the first 200 chars of a particular file
    // =======================================================================
	private static String getCharSnippet(String content) {
		content = content.replaceAll("[\\t\\n\\r]"," ");
		char[] snippet = content.toCharArray();
		String trimmedContent="";
		
		for(int i=0; i<200 && i < snippet.length; i++)
		{
			trimmedContent+=String.valueOf(snippet[i]);
		}
		
		return trimmedContent;
	}

	// ======================================================================
	// writeToFile function to write the sorted list of (term,term_frequency)
	// pairs into a file named sorted_pair_list.txt
	// ====================================================================
	private void writeToFile() {
		try {
			FileWriter fw = new FileWriter(new File("sorted_pair_list.txt"));
			BufferedWriter output = new BufferedWriter(fw);
			String txtLine = "List of unique terms along with the frequency\n";
			txtLine+="=============================================\n";
			for(Map.Entry<String, Long> entry : unique_term.entrySet())
			{
				txtLine+= entry.getKey()+" "+entry.getValue()+"\n";
			}
			output.write(txtLine);
			output.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	// ====================================================================
	// function sort to sort the list of (unique_term,term_frequency) pairs
	// ====================================================================
	private void sort() {
		List<Map.Entry<String, Long>> entries = new ArrayList<Map.Entry<String, Long>>(unique_term.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, Long>>()
				 {
					  public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b){
						  
						  if (a.getValue() < (b.getValue()))
								  return 1;
						  else if (a.getValue() > (b.getValue()))
							  return -1;
						  else
							  return 0;
					  }
					});
		
		// clear the old contents
		unique_term.clear();
		
		// adding the sorted list into the LinkedHashMap
		for(Map.Entry<String, Long> entry : entries)
		{
			unique_term.put(entry.getKey(), entry.getValue());
		}
	}
	
	// ====================================================================
	// getListPairs function to get all the list of (term, term_freq pairs)
	// ====================================================================
	private void getListPairs(IndexReader reader) {
		try {
			Fields fields = MultiFields.getFields(reader);
			Terms terms = fields.terms("contents");
			TermsEnum iterator = terms.iterator(null);
		
			BytesRef byteRef = null;
			while((byteRef = iterator.next()) != null) {
			    String term = byteRef.utf8ToString();
				Term termInstance = new Term("contents", term);	
				long termFreq = reader.totalTermFreq(termInstance);
			    unique_term.put(term,termFreq);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
     * Constructor
     * 
     * @param indexDir
     *            the name of the folder in which the index should be created
     * @throws java.io.IOException
     *             when exception creating index.
     */
    HW4(String indexDir) throws IOException {

	FSDirectory dir = FSDirectory.open(new File(indexDir));

	IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47,sAnalyzer);

	writer = new IndexWriter(dir, config);
    }
    
    

    /**
     * Indexes a file or directory
     * 
     * @param fileName
     *            the name of a text file or a folder we wish to add to the
     *            index
     * @throws java.io.IOException
     *             when exception
     */
    public void indexFileOrDirectory(String fileName) throws IOException {
	// ===================================================
	// gets the list of files in a folder (if user has submitted
	// the name of a folder) or gets a single file name (is user
	// has submitted only the file name)
	// ===================================================
	addFiles(new File(fileName));

	int originalNumDocs = writer.numDocs();
	for (File f : queue) {
	    FileReader fr = null;
	    try {
		Document doc = new Document();

		// ===================================================
		// add contents of file
		// ===================================================
		fr = new FileReader(f);
		String content = stripHTMLTags(fr);
		doc.add(new TextField("contents", content,Field.Store.YES));
		doc.add(new StringField("path", f.getPath(), Field.Store.YES));
		doc.add(new StringField("filename", f.getName(),Field.Store.YES));

		writer.addDocument(doc);
		System.out.println("Added: " + f);		
		
	    } catch (Exception e) {
		System.out.println("Could not add: " + f);
	    } finally {
		fr.close();
	    }
	}

	
	int newNumDocs = writer.numDocs();
	System.out.println("");
	System.out.println("************************");
	System.out
		.println((newNumDocs - originalNumDocs) + " documents added.");
	System.out.println("************************");

	queue.clear();
    }

	// ========================================================================
    // stripHTMLTags function to strip the <html>,</html> and <pre>,</pre> tags
	// ========================================================================
    public String stripHTMLTags(FileReader fr) {
    	StringBuilder content = new StringBuilder();
		BufferedReader ip = new BufferedReader(fr);
		String line;
		try {
			while((line = ip.readLine())!=null)
				content.append(line+"\n");
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		// using Jsoup jar to strip the HTML tags
		return Jsoup.parse(content.toString()).text();
	}

	// =============================================================
    // addFiles function to add the valid files into the queue i.e. 
    // .txt, .xml and .html files, else skip invalid files
	// =============================================================
	private void addFiles(File file) {

	if (!file.exists()) {
	    System.out.println(file + " does not exist.");
	}
	if (file.isDirectory()) {
	    for (File f : file.listFiles()) {
		addFiles(f);
	    }
	} else {
	    String filename = file.getName().toLowerCase();
	    // ===================================================
	    // Only index text files
	    // ===================================================
	    if (filename.endsWith(".htm") || filename.endsWith(".html")
		    || filename.endsWith(".xml") || filename.endsWith(".txt")) {
		queue.add(file);
	    } else {
		System.out.println("Skipped " + filename);
	    }
	}
    }

    /**
     * Close the index.
     * 
     * @throws java.io.IOException
     *             when exception closing
     */
    public void closeIndex() throws IOException {
	writer.close();
    }
}