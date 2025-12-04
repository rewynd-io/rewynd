package io.rewynd.worker.search

import io.rewynd.common.cache.queue.SearchJobHandler
import io.rewynd.common.database.Database
import io.rewynd.common.database.listAllLibraries
import io.rewynd.model.SearchResponse
import io.rewynd.model.SearchResult
import io.rewynd.model.SearchResultType
import io.rewynd.worker.deserializeDirectory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.StoredFields
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.spell.LevenshteinDistance
import org.apache.lucene.search.spell.LuceneDictionary
import org.apache.lucene.search.suggest.analyzing.BlendedInfixSuggester
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Instant

data class SearchIndex(
    val suggester: BlendedInfixSuggester,
    val searcher: IndexSearcher,
    val lastUpdated: Instant,
    val libraryId: String,
)

class SearchHandler(val db: Database) {
    val libIndicies = ConcurrentHashMap<String, SearchIndex>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val jobHander: SearchJobHandler = { context ->
        if (context.request.text.isBlank()) {
            SearchResponse(emptyList())
        } else {
            libIndicies.elements().asIterator().asFlow().flatMapMerge { index ->
                val storedFields = index.searcher.storedFields()
                index.suggester.lookup(context.request.text, false, MAX_RESULTS).asFlow().mapNotNull { lookupResult ->
                    index.searcher.search(
                        TermQuery(Term("title", lookupResult.key.toString())),
                        1,
                    ).scoreDocs.firstOrNull()
                        ?.toSearchResult(storedFields, context.request.text)
                }
            }.let { SearchResponse(it.toList()) }
        }
    }

    suspend fun updateIndicies() {
        this.db.listAllLibraries().mapNotNull { library ->
            val existingIndex = libIndicies[library.name]
            db.getLibraryIndex(library.name, existingIndex?.lastUpdated)?.let { libraryIndex ->
                val index = deserializeDirectory(libraryIndex.index)
                val indexReader = DirectoryReader.open(index)
                val dictionary = LuceneDictionary(indexReader, "title")
                val suggester = BlendedInfixSuggester(index, StandardAnalyzer()).apply { build(dictionary) }
                val searcher = IndexSearcher(indexReader)
                SearchIndex(suggester, searcher, libraryIndex.lastUpdated, libraryIndex.libraryId)
            }
        }.collect {
            libIndicies[it.libraryId] = it
        }
    }
    companion object {
        const val MAX_RESULTS = 100
    }
}

private val levenshteinDistance = LevenshteinDistance()

private fun ScoreDoc.toSearchResult(
    storedFields: StoredFields,
    searchText: String,
): SearchResult {
    val doc = storedFields.document(this.doc)
    val type = SearchResultType.valueOf(doc.get("type"))
    val description = doc.get("description")
    val title = doc.get("title")
    val id = doc.get("id")
    return SearchResult(
        resultType = type,
        id = id,
        title = title,
        description = description,
        score = levenshteinDistance.getDistance(title, searchText).toDouble(),
    )
}
