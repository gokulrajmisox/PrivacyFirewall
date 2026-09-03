package com.example.privacyfirewall.domain

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

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

    fun tokenize(text: String): TokenizationResult {
        // Lowercase for uncased BERT
        val cleanText = text.lowercase()
        
        // Very basic word splitting (splits by space and punctuation)
        // For a true BERT tokenizer we would split by all punctuation keeping the punctuation as separate tokens.
        val tokens = mutableListOf<String>()
        val currentWord = StringBuilder()
        
        for (char in cleanText) {
            if (char.isWhitespace()) {
                if (currentWord.isNotEmpty()) {
                    tokens.add(currentWord.toString())
                    currentWord.clear()
                }
            } else if (isPunctuation(char)) {
                if (currentWord.isNotEmpty()) {
                    tokens.add(currentWord.toString())
                    currentWord.clear()
                }
                tokens.add(char.toString())
            } else {
                currentWord.append(char)
            }
        }
        if (currentWord.isNotEmpty()) {
            tokens.add(currentWord.toString())
        }

        val tokenIds = mutableListOf<Long>()
        val originalWords = mutableListOf<String>()
        
        tokenIds.add(clsTokenId)
        originalWords.add("[CLS]")
        
        for (token in tokens) {
            val subTokens = wordPieceTokenize(token)
            for (subToken in subTokens) {
                tokenIds.add(vocab[subToken] ?: unkTokenId)
                originalWords.add(subToken)
            }
        }
        
        tokenIds.add(sepTokenId)
        originalWords.add("[SEP]")

        // Attention mask is 1 for all real tokens
        val attentionMask = LongArray(tokenIds.size) { 1L }

        return TokenizationResult(tokenIds.toLongArray(), attentionMask, originalWords)
    }

    private fun wordPieceTokenize(word: String): List<String> {
        val outputTokens = mutableListOf<String>()
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
            outputTokens.add(curStr)
            start = end
        }
        
        if (isBad) {
            return listOf("[UNK]")
        }
        return outputTokens
    }

    private fun isPunctuation(char: Char): Boolean {
        val cp = char.code
        if (cp in 33..47 || cp in 58..64 || cp in 91..96 || cp in 123..126) return true
        return !char.isLetterOrDigit() && !char.isWhitespace()
    }
}

data class TokenizationResult(
    val inputIds: LongArray,
    val attentionMask: LongArray,
    val tokens: List<String>
)
