import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TopDocs
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class LuceneLab6(private val indexPath: String) {

    /**
     * Creates objects of class: IndexReader, IndexSearcher
     * @see https://kotlinlang.org/docs/reference/delegated-properties.html
     */
    val indexSearcher: IndexSearcher by lazy {
        val path: Path? = Paths.get(indexPath)
        val directory: Directory = FSDirectory.open(path)
        val indexReader: IndexReader = DirectoryReader.open(directory)
        IndexSearcher(indexReader)
    }

    /**
     * Create the index, fills it with documents (using indexDoc function), closes the index
     * use: indexWriter to create an index of selected files
     */
    fun createIndex(path: String) {
        val iPath = Paths.get(indexPath)
        val directory = FSDirectory.open(iPath)
        val analyzer = StandardAnalyzer()
        val indexWriterConfig = IndexWriterConfig(analyzer)
        indexWriterConfig.openMode = IndexWriterConfig.OpenMode.CREATE
        val indexWriter = IndexWriter(directory, indexWriterConfig)
        val files = File(path).listFiles()
        for (file in files) {
            val document = indexDoc(file.path)
            indexWriter.addDocument(document)
        }
        indexWriter.close()
    }

    /**
     * Create object of class Document; create some necessary fields (e.g. path, content)
     * Call this function in createIndex() to create Documents that would be subsequently added to the index
     */
    fun indexDoc(documentPath: String): Document {
        return Document().apply {
            add(StringField("path", documentPath, Field.Store.YES))
            add(TextField("content", InputStreamReader(FileInputStream(documentPath))))
        }
    }

    /**
     * Create objects of class: Analyzer, IndexSearcher, QueryParser, Query and Hits
     * for Analyzer use standard analyzer
     * for QueryParser indicate the fields to be analyzed
     * for Query you should parse "queryString" which is given as a parameter of the function
     * for TopDocs you should search results (indexSearcher) for a given query and return 5 best documents
     */
    fun processQuery(queryString: String): Array<ScoreDoc>? {
        val analyzer: Analyzer? = StandardAnalyzer()
        val queryParser = QueryParser("content", analyzer)
        val query: Query = queryParser.parse(queryString)
        val topDocs: TopDocs = indexSearcher.search(query, 5)
        return topDocs.scoreDocs
    }
}


fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Edit configurations... -> Program arguments -> Shakespeare index")
        println("need two args with paths to the collection of texts and to the directory where the index would be stored, respectively")
        System.exit(1)
    }
    try {
        val textsPath = args[0]
        val lucene = LuceneLab6(args[1])
        lucene.createIndex(textsPath)
        var query: String

        //process queries until one writes "lab6"
        while (true) {
            val sc = Scanner(System.`in`)
            println("Please enter your query: (lab9 to quit)")
            query = sc.next()

            if (query == "lab9") {
                break
            } //to quit

            val hits = lucene.processQuery(query)

            if (hits != null) {
                println(hits.size.toString() + " result(s) found")

                for (hit in hits) {
                    try {

                        // read off and write its name or path (access from indexSearcher)
                        // and similarity score to the Console
                        println("${lucene.indexSearcher.doc(hit.doc).getField("path").stringValue()} :" +
                                " ${lucene.indexSearcher.getSimilarity(true)}")

                    } catch (e: Exception) {
                        System.err.println("Unexpected exception")
                        System.err.println(e.toString())
                    }

                }

            } else {
                println("Processing the query still not implemented, heh?")
            }
        }

    } catch (e: Exception) {
        System.err.println("Even more unexpected exception")
        System.err.println("Gościu odpal ./gradlew downloadAndUnzipFile")
        System.err.println(e.toString())
    }

}