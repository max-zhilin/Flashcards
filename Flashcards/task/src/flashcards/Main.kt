/*
 * Copyright 2021 Max Zhilin
 * Use of this source code is governed by the X11 License.
 */

package flashcards

import java.io.File
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable


fun main(args: Array<String>) {
    val options = Options.parse(args)

    val cards = Cards()

    options.importFileName?.let {
        cards.import(it)?.also { n -> println("$n cards have been loaded.") }
    }

    do {
        println("Input the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):")
        val action = readLine()!!
        when (action) {
            "add" -> {
                println("The card:")
                val term = readLine()!!
                if (cards.contains("term", term)) {
                    println("The card \"$term\" already exists.")
                    continue
                }

                println("The definition of the card:")
                val definition = readLine()!!
                if (cards.contains("definition", definition)) {
                    println("The definition \"$definition\" already exists.")
                    continue
                }

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
                    val (term, card) = cards.getRandomCard()
                    println("Print the definition of \"$term\":")
                    val answer = readLine()!!
                    if (answer == card.definition)
                        println("Correct!")
                    else with(cards.lookUpOrNull("definition", answer)) {
                        when (this) {
                            null -> println("Wrong. The right answer is \"${card.definition}\".")
                            else -> println("Wrong. The right answer is \"${card.definition}\", but your definition is correct for \"$this\".")
                        }
                        card.mistakes++
                    }
                }
            }
            "log" -> {
                println("File name:")
                val fileName = readLine()!!
                Log.save(fileName)
                println("The log has been saved.")
            }
            "hardest card" -> {
                val hardestCardsMap = cards.getHardestCards()
                when (hardestCardsMap.size) {
                    0 -> println("There are no cards with errors.")
                    1 -> with (hardestCardsMap.entries.first()) {
                        println("The hardest card is \"$key\". You have ${value.mistakes} errors answering it.")
                    }
                    else -> {
                        val terms = hardestCardsMap.map { it.key }.joinToString(", ") { "\"$it\"" }
                        val mistakes = hardestCardsMap.entries.first().value.mistakes
                        println("The hardest cards are $terms. You have $mistakes errors answering them.")
                    }
                }
            }
            "reset stats" -> {
                cards.resetStats()
                println("Card statistics have been reset.")
            }
        }
    } while (action != "exit")

    println("Bye bye!")

    options.exportFileName?.let {
        cards.export(it).also { n -> println("$n cards have been saved.") }
    }
}

class Cards {
    private val cardsMap = mutableMapOf<String, Card>()

    fun contains(part: String, text: String): Boolean = lookUpOrNull(part, text) != null

    fun lookUpOrNull(part: String, text: String): String? {
        return if (part == "term")
            cardsMap[text]?.definition
        else
            cardsMap.getKeyByDefinition(text)
    }

    fun add(term: String, definition: String) {
        cardsMap[term] = Card(definition)
    }

    fun remove(term: String): Boolean = cardsMap.remove(term) != null

    fun getRandomCard(): Pair<String, Card> = cardsMap.entries.random().toPair()

    fun import(fileName: String): Int? {
        try {
            File(fileName).inputStream().use {
                val ois = ObjectInputStream(it)
                val hashMap = ois.readObject() as HashMap<String, Card>
                ois.close()
                cardsMap.putAll(hashMap)
                return hashMap.size
            }
        } catch (e: IOException) {
            return null
        }
    }

    fun export(fileName: String): Int {
        File(fileName).outputStream().use {
            val oos = ObjectOutputStream(it)
            val hashMap = HashMap(cardsMap)
            oos.writeObject(hashMap)
            oos.close()
        }
        return cardsMap.size
    }

    fun getHardestCards(): Map<String, Card> {
        val list = cardsMap.map { it.value.mistakes }
        val max = list.maxOrNull()
        return cardsMap.filterValues { it.mistakes == max && it.mistakes > 0}
    }

    fun resetStats() {
        cardsMap.forEach { it.value.mistakes = 0 }
    }
}

data class Card(val definition: String, var mistakes: Int = 0) : Serializable

fun Map<String, Card>.getKeyByDefinition(definition: String): String? {
    for (element in this) if (element.value.definition == definition) return element.key
    return null
}

fun println(str: String) {
    kotlin.io.println(str)
    Log.add(str)
}

fun readLine(): String? {
    val str = kotlin.io.readLine()
    str?.let { Log.add(it) }
    return str
}

object Log : ArrayList<String>() {

    fun save(fileName: String) {
        File(fileName).writeText(this.joinToString("\n"))
    }
}

class Options private constructor(val importFileName: String?, val exportFileName: String?) {

    companion object {
        fun parse(args: Array<String>): Options {
            var importOption: String? = null
            var exportOption: String? = null
            var i = 0
            while (i < args.size) {
                if (args[i] == "-import") {
                    importOption = args[++i]
                }
                if (args[i] == "-export") {
                    exportOption = args[++i]
                }

                i++
            }

            return Options(importOption, exportOption)
        }
    }
}