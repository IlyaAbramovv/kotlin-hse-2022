package homework03.json

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommentInfo(
    val body: String?,
    val ups: Int,
    val downs: Int,
    val created: Long,
    val id: String,
    val author: String?,
    val depth: Int
)