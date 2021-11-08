package flashcards

fun main() {
    println("Input the number of cards:")
    val n = readLine()!!.toInt()

    val cards = Cards(n) { index ->
        println("Card #${index + 1}:")
        val term = input(this, "term")

        println("The definition for card #${index + 1}:")
        val definition = input(this, "definition")

        Card(term, definition)  // return to init Cards(n)
    }

    for (card in cards) {
        println("Print the definition of \"${card.term}\":")
        val answer = readLine()!!
        if (answer == card.definition)
            println("Correct!")
        else with (cards.lookUpBy("definition", answer, card.term)) {
            when(this) {
                is NotFound -> println("Wrong. The right answer is \"${card.definition}\".")
                is Found ->
                    println("Wrong. The right answer is \"${card.definition}\", but your definition is correct for \"${this.card.term}\".")
            }
        }
    }
}

fun input(cards: Cards, part: String): String {
    var text = readLine()!!
    do {
        val searchResult = cards.lookUpBy(part, text)
        if (searchResult is Found) {
            println("The $part \"$text\" already exists. Try again:")
            text = readLine()!!
        }
    } while (searchResult is Found)
    return text
}
class Cards(size: Int, init: Cards.(index: Int) -> Card) : Iterable<Card> {
    private val cardsList = mutableListOf<Card>()

    init {
        repeat(size) { index -> cardsList.add(init(index)) }
    }
    override fun iterator(): Iterator<Card> = cardsList.iterator()

    fun lookUpBy(part: String, text: String, exceptTerm: String? = null): SearchResult {
        val cardOrNull = cardsList.find { it.term != exceptTerm
                && text == if (part == "term") it.term else it.definition }

        return if (cardOrNull == null) NotFound else Found(cardOrNull)
    }
}

sealed class SearchResult
object NotFound : SearchResult()
class Found(val card: Card) : SearchResult()

data class Card(val term: String, val definition: String)