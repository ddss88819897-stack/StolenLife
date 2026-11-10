package kr.paranos.stolenlife.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** DataStore 에 JSON 으로 저장할 GameState 가 칩이 박힌 메시지까지 그대로 왕복하는지. */
class GameStateSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun stateWithRichMessagesSurvivesRoundTrip() {
        val beat = GameRepository.beats.getValue(StoryEvent.DocumentOpened(DocId.SETTLEMENT))
        val state = GameRepository.demoState(DemoJump.BEFORE_ENDING).copy(
            pin = PinnedEvidence(PinId.BURN_RECORD, "한도윤: 선천적 왼손잡이", "잊혀진 섬"),
            messages = beat.watson.map { ChatMessage(it.sender, it.text, "23:40", it.rich, it.action) },
            glancedSpots = setOf(RoomSpot.DOOR, RoomSpot.BED),
        )

        val restored = json.decodeFromString<GameState>(json.encodeToString(state))

        assertEquals(state, restored)
    }

    @Test
    fun saveFromOlderBuildWithMissingFieldsStillLoads() {
        val restored = json.decodeFromString<GameState>("""{"chapter":2,"chips":["KANG_MINWOO"],"unknownFlag":1}""")
        assertEquals(2, restored.chapter)
        assertEquals(listOf(Chip.KANG_MINWOO), restored.chips)
        assertEquals(31, restored.battery)
    }
}
