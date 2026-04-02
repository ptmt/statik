package com.potomushto.statik.cms

import kotlin.test.Test
import kotlin.test.assertTrue

class CmsWebAssetsTest {

    @Test
    fun `index html reserves chip space before status loads`() {
        val html = CmsWebAssets.indexHtml("Demo", "/__statik__/cms")

        assertTrue(html.contains("""id="status-chips" class="status-strip status-strip-loading""""))
        assertTrue(html.contains("""<span class="chip chip-skeleton" aria-hidden="true">000 dirty</span>"""))
        assertTrue(html.contains("""<span class="chip chip-skeleton" aria-hidden="true">synced 00:00</span>"""))
    }

    @Test
    fun `styles keep status strip height stable`() {
        val styles = CmsWebAssets.stylesCss

        assertTrue(styles.contains("min-height: 32px;"))
        assertTrue(styles.contains(".status-strip-loading .chip-skeleton"))
    }

    @Test
    fun `app script clears loading state after status render`() {
        val script = CmsWebAssets.appJs

        assertTrue(script.contains("""elements.status.classList.remove("status-strip-loading");"""))
        assertTrue(script.contains("""elements.status.removeAttribute("aria-busy");"""))
    }

    @Test
    fun `app script save flow does not reload the editor after save`() {
        val script = CmsWebAssets.appJs

        assertTrue(script.contains("""const savedSnapshot = savedDocumentSnapshot(response.item);"""))
        assertTrue(script.contains("""rememberSavedSnapshot("Saved", savedSnapshot);"""))
        assertTrue(!script.contains("""await openEntry(response.item.sourcePath, { logLoad: false });"""))
    }
}
