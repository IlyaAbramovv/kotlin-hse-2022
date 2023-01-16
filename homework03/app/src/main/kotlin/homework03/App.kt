package homework03

import homework03.serializer.CsvFileWriter
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope


fun main(args: Array<String>) = runBlocking {
    if (args.isEmpty())
        throw IllegalArgumentException(
            "No arguments given. Add at least one reddit topic name to get info about it"
        )
    val client = RedditClient()
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Cant create file; got: $exception")
    }

    val httpClient = HttpClient(CIO)
    supervisorScope {
        for (topicName in args) {
            launch(handler) {
                val topic = client.getTopic(topicName, httpClient)
                CsvFileWriter.writeTopicAndComments(topic, topicName, client, httpClient)
            }
        }
    }

}

