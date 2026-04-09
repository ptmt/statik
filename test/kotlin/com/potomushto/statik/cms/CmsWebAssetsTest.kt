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
        assertTrue(script.contains("""applyContentSnapshot(savedSnapshot);"""))
        assertTrue(script.contains("""rememberSavedSnapshot("Saved", savedSnapshot);"""))
        assertTrue(!script.contains("""await openEntry(response.item.sourcePath, { logLoad: false });"""))
    }

    @Test
    fun `app script saves from snapshots and aborts stale autosaves`() {
        val script = CmsWebAssets.appJs

        assertTrue(script.contains("""const AUTOSAVE_DELAY_MS = 5000;"""))
        assertTrue(script.contains("""let contentDraftSnapshot = null;"""))
        assertTrue(script.contains("""signal: controller.signal"""))
        assertTrue(script.contains("""frontmatter: snapshot.frontmatter,"""))
        assertTrue(script.contains("""body: snapshot.body"""))
        assertTrue(script.contains("""const currentSnapshot = currentContentSnapshot();"""))
        assertTrue(script.contains("""if (!currentSnapshot || currentSnapshot.key !== snapshot.key) {"""))
        assertTrue(script.contains("""elements.source.addEventListener("input", () => {"""))
        assertTrue(script.contains("""captureContentSnapshot();"""))
        assertTrue(!script.contains("""const parsed = parseDocument(elements.source.value);"""))
    }

    @Test
    fun `app script updates browser title from editor heading`() {
        val script = CmsWebAssets.appJs

        assertTrue(script.contains("""const baseDocumentTitle = document.title || "Statik CMS";"""))
        assertTrue(script.contains("""document.title = nextTitle && nextTitle !== "Select a file""""))
        assertTrue(script.contains("""? nextTitle + " · " + baseDocumentTitle"""))
        assertTrue(script.contains("""setEditorHeading(fileNameFromPath(normalizedPath), elements.editorSubtitle.textContent || "");"""))
    }
}
