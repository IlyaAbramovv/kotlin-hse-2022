package homework03

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.*
import homework03.json.*
import homework03.json.Listing
import homework03.json.SingleCommentSnapshot
import homework03.json.TopicSnapshot
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*

class RedditClient {
    private val httpClient = HttpClient(CIO)

    private suspend fun requestData(link: String, name: String): String {
        val request = httpClient.request(link)
        if (!request.status.isSuccess()) throw RedditClientException.PageGettingException(name)
        return request.bodyAsText()
    }

    suspend fun getTopic(name: String): TopicSnapshot =

        coroutineScope {
            val mapper = jacksonObjectMapper()
            val mapperWrapper = JacksonMapperWrapper(mapper, name)

            val json = async {
                requestData(RedditUrlBuilder.getJsonPageUrl(name), name)
            }
            val jsonInfo = async {
                requestData(RedditUrlBuilder.getJsonAboutPageUrl(name), name)
            }

            val discussions = async {
                val discussionsInfo: Listing = mapperWrapper.readValue(json.await())
                discussionsInfo.data.children.map { it.data }
            }

            val info: JsonSubredditInfoRepresentation = mapperWrapper.readValue(jsonInfo.await())
            val subredditInfo = info.data
            TopicSnapshot(
                subredditInfo.creationTime,
                subredditInfo.subscribersOnline,
                subredditInfo.public_description,
                discussions.await(),
                subredditInfo.id
            )
        }

    suspend fun getComments(name: String): CommentsSnapshot {
        val request = httpClient.request(RedditUrlBuilder.getJsonPageUrlById(name))
        if (!request.status.isSuccess()) throw RedditClientException.PageGettingException(name)
        val json = request.bodyAsText()
        val mapper = jacksonObjectMapper()
        val commentsTree: JsonNode = mapper.readTree(json)
        val commentsList: MutableList<SingleCommentSnapshot> = ArrayList()

        commentsTree[1]?.get("data")?.get("children") ?: throw RedditClientException.JsonUnexpectedStructureException(name)
        for (comment in commentsTree[1]["data"]["children"]) {
            val data = comment["data"] ?: throw RedditClientException.JsonUnexpectedStructureException(name)
            addComments(data, name, mapper, commentsList, name)
        }
        return CommentsSnapshot(commentsList)
    }

    private fun addComments(
        parent: JsonNode,
        replyTo: String,
        mapper: ObjectMapper,
        commentsList: MutableList<SingleCommentSnapshot>,
        name: String
    ) {
        val commentInfo: CommentInfo =
            mapper.treeToValue(parent) ?: throw RedditClientException.JsonParsingException()
        commentsList.add(
            SingleCommentSnapshot(
                commentInfo.created,
                commentInfo.ups,
                commentInfo.downs,
                commentInfo.body,
                commentInfo.author,
                commentInfo.id,
                replyTo,
                commentInfo.depth
            )
        )
        if (parent["replies"] != null && !parent["replies"].isEmpty) {
            parent.get("replies")?.get("data")?.get("children")
                ?: throw RedditClientException.JsonUnexpectedStructureException(name)
            for (reply in parent["replies"]["data"]["children"]) {
                val data = reply["data"]
                    ?: throw RedditClientException.JsonUnexpectedStructureException(name)
                addComments(data, commentInfo.id, mapper, commentsList, name)
            }
        }
    }
}

object RedditUrlBuilder {
    private const val redditLink = "https://www.reddit.com"
    private const val jsonPageSuffix = ".json"
    private const val jsonAboutPageSuffix = "about.json"
    internal fun getJsonPageUrl(name: String) = "$redditLink/r/$name/$jsonPageSuffix"
    internal fun getJsonAboutPageUrl(name: String) = "$redditLink/r/$name/$jsonAboutPageSuffix"
    internal fun getJsonPageUrlById(id: String) = "$redditLink/$id/$jsonPageSuffix"
}

class JacksonMapperWrapper(val mapper: ObjectMapper, val topicName: String) {
    inline fun <reified T> readValue(content: String): T {
        try {
            return mapper.readValue(content, jacksonTypeRef<T>())
        } catch (e: JsonMappingException) {
            throw RedditClientException.JsonParsingException()
        } catch (e: MissingKotlinParameterException) {
            throw RedditClientException.JsonParsingException("Error in $topicName: Invalid Json file given")
        }
    }
}

sealed class RedditClientException(reason: String) : RuntimeException(reason) {

    class PageGettingException(name: String) : RedditClientException("Can't get page with name \"$name\"")

    open class JsonParsingException(name: String = "Something went wrong during parsing json data") :
        RedditClientException(name)

    class JsonUnexpectedStructureException(name: String) :
        JsonParsingException("Unexpected structure in json data got from page with name \"$name\"")
}