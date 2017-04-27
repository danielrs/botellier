package org.botellier.protocol.command.parser

// see: https://gist.github.com/absurdhero/24e6b0ed914b0499a38f
// see: https://blogs.msdn.microsoft.com/lukeh/2007/08/19/monadic-parser-combinators-using-c-3-0/
data class Result<TInput, TOutput>(val value: TOutput, val rest: TInput)

class Parser<TInput, TOutput>(val f: (TInput) -> Result<TInput, TOutput>?) {
    operator fun invoke(input: TInput): Result<TInput, TOutput>? = f(input)

    infix fun or(other: Parser<TInput, TOutput>): Parser<TInput, TOutput> {
        return Parser({ input -> this(input) ?: other(input) })
    }

    infix fun and(other: Parser<TInput, TOutput>): Parser<TInput, TOutput> {
        return Parser({ input ->
            val result = this(input)
            if (result != null) {
                other(result.rest)
            }
            else {
                null
            }
        })
    }

    infix fun <TOutputNew>then(other: Parser<TInput, TOutputNew>): Parser<TInput, TOutputNew> {
        return Parser({ input ->
            val result = this(input)
            if (result != null) {
                other(result.rest)
            }
            else {
                null
            }
        })
    }

    fun filter(pred: (TOutput) -> Boolean): Parser<TInput, TOutput> {
        return Parser({ input ->
            var result = this(input)
            if (result != null && pred(result.value)) {
                result
            }
            else {
                null
            }
        })
    }

    fun <ToutputNew> map(f: (TOutput) -> ToutputNew): Parser<TInput, ToutputNew> {
        return Parser({ input ->
            var result = this(input)
            if (result != null) {
                Result(f(result.value), result.rest)
            }
            else {
                null
            }
        })
    }

    fun <TOutputMid, TOutputNew> flatMap(
            f: (TOutput) -> Parser<TInput, TOutputMid>,
            transform: (TOutput, TOutputMid) -> TOutputNew): Parser<TInput, TOutputNew> {
        return Parser({ input ->
            var r0 = this(input)
            if (r0 != null) {
                val r1 = f(r0.value)(r0.rest)
                if (r1 != null) {
                    Result(transform(r0.value, r1.value), r1.rest)
                }
                else {
                    null
                }
            }
            else {
                null
            }
        })
    }
}
