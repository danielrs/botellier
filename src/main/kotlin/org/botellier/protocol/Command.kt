package org.botellier.protocol

enum class CommandType {
    // Connection
    AUTH,
    ECHO,
    PING,
    QUIT,
    SELECT,

//    // Hashes
//    HDEL,
//    HEXISTS,
//    HGET,
//    HGETALL,
//    HINCRBY,
//    HINCRBYFLOAT,
//    HKEYS,
//    HLEN,
//    HMGET,
//    HMSET,
//    HSET,
//    HSETNX,
//    HSTRLEN,
//    HVALS,
//    HSCAN,

    // Keys
    DEL,
    DUMP,
    EXISTS,
    KEYS,
    RENAME,
    RENAMENX,
    SORT,
    TOUCH,
    TYPE,
}
