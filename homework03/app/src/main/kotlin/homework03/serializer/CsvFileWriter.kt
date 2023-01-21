package homework03.serializer

import com.soywiz.korio.file.VfsFile
import homework03.json.DiscussionInformation
import homework03.json.SingleCommentSnapshot
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.file.useVfs
import homework03.RedditClient
import homework03.json.TopicSnapshot

internal class CsvFileWriter {
    companion object {
        private suspend fun writeTopics(topic: TopicSnapshot, name: String, cwd: VfsFile) {
            cwd["$name-${topic.requestTime}-subjects.csv"].useVfs {
                coroutineScope {
                    launch {
                        it.writeString(
                            csvSerialize(
                                topic.reachableDiscussionsInfo,
                                DiscussionInformation::class
                            )
                        )
                    }
                }
            }
        }

        private suspend fun writeComments(
            topic: TopicSnapshot,
            name: String,
            cwd: VfsFile,
            redditClient: RedditClient
        ) {
            cwd["$name-${topic.requestTime}-comments.csv"].useVfs {
                val reachableDiscussionsIds = topic.reachableDiscussionsInfo.map { it.id }
                coroutineScope {
                    for (commentId in reachableDiscussionsIds) {
                        launch {
                            val comment = redditClient.getComments(commentId)
                            it.writeString(csvSerialize(comment.comments, SingleCommentSnapshot::class))
                        }
                    }
                }
            }
        }

        suspend fun writeTopicAndComments(
            topic: TopicSnapshot,
            name: String,
            redditClient: RedditClient
        ) {
            val cwd = localCurrentDirVfs
            coroutineScope {
                launch {
                    writeTopics(topic, name, cwd)
                }
                launch {
                    writeComments(topic, name, cwd, redditClient)
                }
            }
        }
    }
}