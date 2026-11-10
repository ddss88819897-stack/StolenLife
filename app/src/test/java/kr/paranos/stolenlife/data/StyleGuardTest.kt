package kr.paranos.stolenlife.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 디자인 규칙을 코드로 못 박는다.
 *  - 컬러 이모지 0개 (보조 평면 문자 · 이모지 변형 선택자 금지)
 *  - RoundedCornerShape 사용 금지
 * 누가 나중에 🔒 하나를 넣어도 빌드가 빨갛게 깨진다.
 */
class StyleGuardTest {

    private val sources: List<File> = listOf("src/main/java", "src/main/res")
        .map(::File)
        .filter(File::exists)
        .flatMap { root -> root.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }.toList() }

    @Test
    fun sourcesAreVisibleToTheGuard() {
        assertTrue("src/main 을 찾지 못함. 작업 디렉터리 확인", sources.any { it.name == "GameRepository.kt" })
    }

    @Test
    fun noColorEmojiAnywhere() {
        val offenders = sources.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val bad = line.codePoints().anyMatch { it >= 0x1F000 || it == 0xFE0F || it == 0x20E3 }
                if (bad) "${file.path}:${index + 1}" else null
            }
        }
        assertTrue("이모지 발견: $offenders", offenders.isEmpty())
    }

    @Test
    fun noRoundedCorners() {
        val token = "Rounded" + "CornerShape"
        val offenders = sources.filter { it.extension == "kt" && token in it.readText() }.map { it.path }
        assertTrue("둥근 모서리 금지: $offenders", offenders.isEmpty())
    }
}
