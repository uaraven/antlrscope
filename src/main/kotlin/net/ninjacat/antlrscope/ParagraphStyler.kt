package net.ninjacat.antlrscope

import javafx.application.Platform
import org.fxmisc.richtext.GenericStyledArea
import org.fxmisc.richtext.model.Paragraph
import org.fxmisc.richtext.model.StyleSpans
import org.reactfx.collection.ListModification
import java.util.function.Consumer


class ParagraphStyler<PS, SEG, S>(
    private val area: GenericStyledArea<PS, SEG, S>,
    private val computeStyles: (String) -> StyleSpans<S>
) :
    Consumer<ListModification<out Paragraph<PS, SEG, S>?>> {
    private var prevParagraph = 0
    private var prevTextLength = 0
    override fun accept(lm: ListModification<out Paragraph<PS, SEG, S>?>) {
        if (lm.addedSize > 0) {
            val paragraph = Math.min(area.firstVisibleParToAllParIndex() + lm.from, area.paragraphs.size - 1)
            val text = area.getText(paragraph, 0, paragraph, area.getParagraphLength(paragraph))
            if (paragraph != prevParagraph || text.length != prevTextLength) {
                val startPos = area.getAbsolutePosition(paragraph, 0)
                Platform.runLater { area.setStyleSpans(startPos, computeStyles(text)) }
                prevTextLength = text.length
                prevParagraph = paragraph
            }
        }
    }

}
