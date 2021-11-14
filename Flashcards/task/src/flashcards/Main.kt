/*
 * Copyright 2021 Max Zhilin
 * Use of this source code is governed by the X11 License.
 */

package flashcards

import java.io.File
import java.util.Properties

fun main() {
    val cards = Cards()

    do {
        println("Input the action (add, remove, import, export, ask, exit):")
        val action = readLine()!!
        when (action) {
            "add" -> {
                println("The card:")
                val term = cards.inputOrElse("term") { entered ->
                    println("The card \"$entered\" already exists.")
                    null
                } ?: continue

                println("The definition of the card:")
                val definition = cards.inputOrElse("definition") { entered ->
                    println("The definition \"$entered\" already exists.")
                    null
                } ?: continue

                cards.add(term, definition)
                println("The pair (\"$term\":\"$definition\") has been added.")
            }
            "remove" -> {
                println("Which card?")
                val term = readLine()!!
                if (cards.remove(term)) println("The card has been removed.")
                else println("Can't remove \"$term\": there is no such card.")
            }
            "import" -> {
                println("File name:")
                val fileName = readLine()!!
                val n = cards.import(fileName)
                if (n == null) println("File not found.")
                else println("$n cards have been loaded.")
            }
            "export" -> {
                println("File name:")
                val fileName = readLine()!!
                val n = cards.export(fileName)
                println("$n cards have been saved.")
            }
            "ask" -> {
                println("How many times to ask?")
                val n = readLine()!!.toInt()
                repeat(n) {
                    val (term, definition) = cards.getRandomCard()
                    println("Print the definition of \"$term\":")
                    val answer = readLine()!!
                    if (answer == definition)
                        println("Correct!")
                    else with(cards.lookUpOrNull("definition", answer)) {
                        when (this) {
                            null -> println("Wrong. The right answer is \"$definition\".")
                            else -> println("Wrong. The right answer is \"$definition\", but your definition is correct for \"$this\".")
                        }
                    }
                }
            }
        }
    } while (action != "exit")

    println("Bye bye!")
}

class Cards {
    private val cardsMap = mutableMapOf<String, String>()

    fun inputOrElse(part: String, whenExists: (String) -> String?): String? {
        val text = readLine()!!
        return if (lookUpOrNull(part, text) == null) text
        else whenExists(text)
    }

    fun lookUpOrNull(part: String, text: String): String? {
        return if (part == "term")
            cardsMap[text]
        else
            cardsMap.getKeyByValue(text)
    }

    fun add(term: String, definition: String) {
        cardsMap[term] = definition
    }

    fun remove(term: String): Boolean = cardsMap.remove(term) != null

    fun getRandomCard(): Pair<String, String> = cardsMap.entries.random().toPair()

    fun import(fileName: String): Int? {
        val props = Properties()
        val file = File(fileName)
        try {
            props.load(file.inputStream())
        } catch (e: Exception) {
            return null
        }
        props.forEach {
            cardsMap[it.key as String] = it.value as String
        }
        return props.size
    }

    fun export(fileName: String): Int {
        val props = Properties()
        props.putAll(cardsMap)
        val file = File(fileName)
        props.store(file.outputStream(), null)
        return cardsMap.size
    }
}

fun <K, V> Map<out K, V>.getKeyByValue(value: V): K? {
    for (element in this) if (element.value == value) return element.key
    return null
}