package com.gravasom.feature.record

import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule

internal fun ComposeTestRule.onAllNodesWithTagCompat(tag: String) = onAllNodes(hasTestTag(tag)).fetchSemanticsNodes()

internal fun SemanticsNodeInteractionCollection.assertCountAtLeast(minimo: Int) {
    val n = fetchSemanticsNodes().size
    check(n >= minimo) { "esperava ao menos $minimo, achei $n" }
}
