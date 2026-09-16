package com.futuretech.poweruser.textbook

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Freezes the exact source set which defined "all current tracks combined" when the literal 5x rule
 * was accepted. The target is not allowed to shrink by editing the old corpus after the fact.
 */
class V5FrozenBaselineSourceTest {
    private fun repoFile(path: String): File {
        val candidates = listOf(File(path), File("app/$path"))
        return candidates.firstOrNull { it.isFile }
            ?: error("Cannot locate frozen baseline source: $path")
    }

    private fun gitBlobSha1(file: File): String {
        val bytes = file.readBytes()
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update("blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8))
        digest.update(bytes)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private val frozen = linkedMapOf(
        "src/main/assets/textbook/v3/track_01.md" to "e7b1d3088032c4545152b55bf6c2a667b5f4998f",
        "src/main/assets/textbook/v3/track_02.md" to "183b8406229706d5f7f5ed3120620f5ba559ac80",
        "src/main/assets/textbook/v3/track_03.md" to "48b05f24b0c62c0c12df7c11e115f1bde61460db",
        "src/main/assets/textbook/v3/track_04.md" to "41eaf335fed74fa6c68ac7644dc1e55d176ef1ca",
        "src/main/assets/textbook/v3/track_05.md" to "bb416e69afdeb8c230a9a22139c27e473650e000",
        "src/main/assets/textbook/v3/track_06.md" to "c20f9bff309455d0a1011b50ec92f3fbe76b27a3",
        "src/main/assets/textbook/v3/track_07.md" to "dc72b9d0a6ae9a7e64a1c05621df88053b508c45",
        "src/main/assets/textbook/v3/track_08.md" to "3f5fcad52a93dc7ec1a59494ae27827cd6024885",
        "src/main/assets/textbook/v3/track_09.md" to "db4df6daad67c4898ccbcc357cab47bd79c06120",
        "src/main/assets/textbook/v3/track_10.md" to "024573f9cfbcb181decb643383478d98f88fc808",
        "src/main/assets/textbook/v3/track_11.md" to "29ca117af2ba9dea960e578b3e602a8afdeaa59e",
        "src/main/java/com/futuretech/poweruser/textbook/V1TextbookCatalog.kt" to "724d30f13bb36a2eb7dc4fc6bccd6707e3e76818",
        "src/main/java/com/futuretech/poweruser/textbook/TextbookSectioner.kt" to "575b536a6ab2a90e969a8575df9c5a79693e92eb",
        "src/main/java/com/futuretech/poweruser/textbook/V3BeginnerGuidance.kt" to "5e09d64ba2e021d8347440564ff5c13bba57a2cd",
        "src/main/java/com/futuretech/poweruser/textbook/V3BeginnerGuidanceResolver.kt" to "d5e24d6086d314655adf2087549179834025ec57",
        "src/main/java/com/futuretech/poweruser/textbook/V4BookDepthLibrary.kt" to "d6668cc5a42ff678d5d866a51bf7a257612ae13f",
        "src/main/java/com/futuretech/poweruser/textbook/V4ExpertDepthTrack01.kt" to "1709cde0d11e492320f415d34a38d9a1c62a6d9d",
        "src/main/java/com/futuretech/poweruser/textbook/V4ExpertDepthTrack02.kt" to "6f095fb94a941f7b1565353c3f90cf7c760bcbdb",
        "src/main/java/com/futuretech/poweruser/textbook/V4ExpertDepthTrack03.kt" to "ad557d92361fbee783965a170e079eca9368ff57",
        "src/main/java/com/futuretech/poweruser/textbook/V4ExpertDepthTrack04.kt" to "e327e8004dbc4043a45f190a19a71d56baf3912b",
        "src/main/java/com/futuretech/poweruser/textbook/V4ExpertDepthTrack05.kt" to "3c36201de96e4f8702e8d739f0199c088d351a96",
        "src/main/java/com/futuretech/poweruser/textbook/V4ExpertDepthTrack06.kt" to "060463a62e7416319df233d147c785262f2504b3",
        "src/main/java/com/futuretech/poweruser/textbook/V4ExpertDepthTrack07.kt" to "4d74b7b67493123c13d45c8c5d787b07f70422f1",
        "src/main/java/com/futuretech/poweruser/textbook/V4ExpertDepthTrack08.kt" to "31bb394ed9354d45a5afb8dbf2bdb3735b80a318",
        "src/main/java/com/futuretech/poweruser/textbook/V4ExpertDepthTrack09.kt" to "4b727a545504f784fe59e961f8e917787e0bd5bf",
        "src/main/java/com/futuretech/poweruser/textbook/V4ExpertDepthTrack10.kt" to "819312a5eb2d9b17b5cca221cb8e1746b5957ef6",
        "src/main/java/com/futuretech/poweruser/textbook/V4ExpertDepthTrack11.kt" to "f95b9cda6df7cdd8f92ea2ae50081c14d55cffb2"
    )

    @Test
    fun baselineSourcesCannotBeShrunkOrRewritten() {
        frozen.forEach { (path, expectedSha) ->
            assertEquals("Frozen 5x baseline changed: $path", expectedSha, gitBlobSha1(repoFile(path)))
        }
    }
}
