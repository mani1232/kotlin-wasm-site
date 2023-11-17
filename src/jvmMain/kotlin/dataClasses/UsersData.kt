package dataClasses

import ChatUserImpl
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class ChatUser(
    override val userName: String,
    override var displayName: String,
    override val password: String,
    val sessionTokens: MutableList<String> = mutableListOf(),
) : ChatUserImpl, DataBaseUser()


abstract class DataBaseUser(
    @BsonId val id: ObjectId = ObjectId.get()
)