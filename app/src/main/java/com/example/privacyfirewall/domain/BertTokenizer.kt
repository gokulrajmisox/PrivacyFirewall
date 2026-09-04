package com.example.privacyfirewall.domain

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

data class TokenSpan(
    val token: String,
    val id: Long,
    val start: Int,
    val end: Int
)

data class TokenizationResult(
    val inputIds: LongArray,
    val attentionMask: LongArray,
    val tokens: List<TokenSpan>
)

class BertTokenizer(context: Context) {
    private val vocab = mutableMapOf<String, Long>()
    private val invVocab = mutableMapOf<Long, String>()
    
    val clsTokenId: Long = 101
    val sepTokenId: Long = 102
    val unkTokenId: Long = 100
    val padTokenId: Long = 0

    init {
        context.assets.open("vocab.txt").use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var index = 0L
                var line: String? = reader.readLine()
                while (line != null) {
                    vocab[line] = index
                    invVocab[index] = line
                    index++
                    line = reader.readLine()
                }
            }
        }
    }

    private data class WordSpan(val word: String, val start: Int, val end: Int)

    fun tokenize(text: String): TokenizationResult {
        if (text.isEmpty()) {
            return TokenizationResult(
                longArrayOf(clsTokenId, sepTokenId),
                longArrayOf(1L, 1L),
                listOf(
                    TokenSpan("[CLS]", clsTokenId, 0, 0),
                    TokenSpan("[SEP]", sepTokenId, 0, 0)
                )
            )
        }

        // Split into basic words with real character offsets in the original text
        val words = mutableListOf<WordSpan>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c.isWhitespace()) {
                i++
            } else if (isPunctuation(c)) {
                words.add(WordSpan(c.toString().lowercase(), i, i + 1))
                i++
            } else {
                val start = i
                while (i < text.length && !text[i].isWhitespace() && !isPunctuation(text[i])) {
                    i++
                }
                val word = text.substring(start, i).lowercase()
                words.add(WordSpan(word, start, i))
            }
        }

        val tokenSpans = mutableListOf<TokenSpan>()
        
        // Add [CLS]
        tokenSpans.add(TokenSpan("[CLS]", clsTokenId, 0, 0))

        // WordPiece tokenize each word and preserve exact character offsets
        for (w in words) {
            val subTokens = wordPieceTokenize(w.word, w.start, w.end)
            tokenSpans.addAll(subTokens)
        }

        // Add [SEP]
        tokenSpans.add(TokenSpan("[SEP]", sepTokenId, text.length, text.length))

        val tokenIds = tokenSpans.map { it.id }.toLongArray()
        val attentionMask = LongArray(tokenIds.size) { 1L }

        return TokenizationResult(tokenIds, attentionMask, tokenSpans)
    }

    private fun wordPieceTokenize(word: String, wordStart: Int, wordEnd: Int): List<TokenSpan> {
        val output = mutableListOf<TokenSpan>()
        var isBad = false
        var start = 0

        while (start < word.length) {
            var end = word.length
            var curStr = ""
            var found = false

            while (start < end) {
                val subStr = if (start == 0) word.substring(start, end) else "##" + word.substring(start, end)
                if (vocab.containsKey(subStr)) {
                    curStr = subStr
                    found = true
                    break
                }
                end--
            }

            if (!found) {
                isBad = true
                break
            }

            val id = vocab[curStr] ?: unkTokenId
            // Exact character span in the original text
            val tokenStart = wordStart + start
            val tokenEnd = wordStart + end
            output.add(TokenSpan(curStr, id, tokenStart, tokenEnd))
            start = end
        }

        if (isBad) {
            return listOf(TokenSpan("[UNK]", unkTokenId, wordStart, wordEnd))
        }
        return output
    }

    private fun isPunctuation(char: Char): Boolean {
        val cp = char.code
        if (cp in 33..47 || cp in 58..64 || cp in 91..96 || cp in 123..126) return true
        return !char.isLetterOrDigit() && !char.isWhitespace()
    }
}
