import ezi.lab6.RssFeedDocument
import ezi.lab6.RssFeedParser
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.TopDocs
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.io.StringReader
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*


class LuceneSearchApp(private val indexPath: String) {

    fun index(docs: List<RssFeedDocument>) {

        val iPath = Paths.get(indexPath)
        val directory = FSDirectory.open(iPath)
        val analyzer = StandardAnalyzer()
        val indexWriterConfig = IndexWriterConfig(analyzer)
        indexWriterConfig.openMode = IndexWriterConfig.OpenMode.CREATE
        val indexWriter = IndexWriter(directory, indexWriterConfig)
        for (doc in docs) {
            indexWriter.addDocument(Document().apply {
                add(TextField("title", doc.title, Field.Store.YES))
                add(TextField("description", StringReader(doc.description)))
                add(TextField("pubDate", StringReader(doc.pubDate.time.toString())))
            })
        }
        indexWriter.close()

    }

    val indexSearcher: IndexSearcher by lazy {
        val path: Path? = Paths.get(indexPath)
        val directory: Directory = FSDirectory.open(path)
        val indexReader: IndexReader = DirectoryReader.open(directory)
        IndexSearcher(indexReader)
    }

    fun search(inTitle: List<String>?, notInTitle: List<String>?, inDescription: List<String>?, notInDescription: List<String>?, startDate: String?, endDate: String?): List<String> {
        var queryStr = ""
        if (inTitle != null || notInTitle != null) {
            queryStr += "title:"
            if (inTitle != null)
                queryStr += "${inTitle.joinToString(separator = " ")} "
            if (notInTitle != null)
                queryStr += "- ${notInTitle.joinToString(separator = " ")} "
        }
        if (inDescription != null || notInDescription != null) {
            queryStr += "description:"
            if (inDescription != null)
                queryStr += "${inDescription.joinToString(separator = " ")} "
            if (notInDescription != null)
                queryStr += "- ${notInDescription.joinToString(separator = " ")} "
        }
        if (startDate != null && endDate != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val startLong = sdf.parse(startDate).time
            val end: Date = sdf.parse(endDate)
            val c = Calendar.getInstance()
            c.time = end
            c.add(Calendar.DATE, 1)
            queryStr += "pubDate:{$startLong TO ${c.timeInMillis}} "
        }

        printQuery(inTitle, notInTitle, inDescription, notInDescription, startDate, endDate)

        val analyzer: Analyzer? = StandardAnalyzer()
        val queryParser = MultiFieldQueryParser(arrayOf("title", "pubDate", "description"), analyzer)

        val query: Query = queryParser.parse(queryStr)
        val topDocs: TopDocs = indexSearcher.search(query, 5)
        val hits = topDocs.scoreDocs
        val list = LinkedList<String>()
        for (hit in hits) {
            list.add(indexSearcher.doc(hit.doc).getField("title").stringValue())
        }
        return list
    }

    private fun printQuery(inTitle: List<String>?, notInTitle: List<String>?, inDescription: List<String>?, notInDescription: List<String>?, startDate: String?, endDate: String?) {
        print("Search (")
        if (inTitle != null) {
            print("in title: $inTitle")
            if (notInTitle != null || inDescription != null || notInDescription != null || startDate != null || endDate != null)
                print("; ")
        }
        if (notInTitle != null) {
            print("not in title: $notInTitle")
            if (inDescription != null || notInDescription != null || startDate != null || endDate != null)
                print("; ")
        }
        if (inDescription != null) {
            print("in description: $inDescription")
            if (notInDescription != null || startDate != null || endDate != null)
                print("; ")
        }
        if (notInDescription != null) {
            print("not in description: $notInDescription")
            if (startDate != null || endDate != null)
                print("; ")
        }
        if (startDate != null) {
            print("startDate: $startDate")
            if (endDate != null)
                print("; ")
        }
        if (endDate != null)
            print("endDate: $endDate")
        println("):")
    }

    fun printResults(results: List<String>) {
        if (results.isNotEmpty()) {
            results.toMutableList().sort()
            for (i in results.indices)
                println(" " + (i + 1) + ". " + results[i])
        } else
            println(" no results")
    }
}

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        val engine = LuceneSearchApp("RSSIndex")

        val parser = RssFeedParser()
        parser.parse(args[0])
        val docs = parser.documents

        engine.index(docs)

        var inTitle: MutableList<String>
        val notInTitle: MutableList<String>
        val inDescription: MutableList<String>
        var notInDescription: MutableList<String>
        var results: List<String>

        // 1) search documents with words "kim" and "korea" in the title
        inTitle = LinkedList()
        inTitle.add("kim")
        inTitle.add("korea")
        results = engine.search(inTitle, null, null, null, null, null)
        engine.printResults(results)

        // 2) search documents with word "kim" in the title and no word "korea" in the description
        inTitle = LinkedList()
        notInDescription = LinkedList()
        inTitle.add("kim")
        notInDescription.add("korea")
        results = engine.search(inTitle, null, null, notInDescription, null, null)
        engine.printResults(results)

        // 3) search documents with word "us" in the title, no word "dawn" in the title and word "" and "" in the description
        inTitle = LinkedList()
        inTitle.add("us")
        notInTitle = LinkedList()
        notInTitle.add("dawn")
        inDescription = LinkedList()
        inDescription.add("american")
        inDescription.add("confession")
        results = engine.search(inTitle, notInTitle, inDescription, null, null, null)
        engine.printResults(results)

        // 4) search documents whose publication date is 2011-12-18
        results = engine.search(null, null, null, null, "2011-12-18", "2011-12-18")
        engine.printResults(results)

        // 5) search documents with word "video" in the title whose publication date is 2000-01-01 or later
        inTitle = LinkedList()
        inTitle.add("video")
        results = engine.search(inTitle, null, null, null, "2000-01-01", null)
        engine.printResults(results)

        // 6) search documents with no word "canada" or "iraq" or "israel" in the description whose publication date is 2011-12-18 or earlier
        notInDescription = LinkedList()
        notInDescription.add("canada")
        notInDescription.add("iraq")
        notInDescription.add("israel")
        results = engine.search(null, null, null, notInDescription, null, "2011-12-18")
        engine.printResults(results)
    } else
        println("ERROR: the path of a RSS Feed file has to be passed as a command line argument.")
}
