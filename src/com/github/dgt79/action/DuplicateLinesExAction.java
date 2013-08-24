package com.github.dgt79.action;


import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.util.Pair;

public class DuplicateLinesExAction extends EditorAction {

	protected DuplicateLinesExAction() {
		super(new EditorActionHandler() {
			@Override
			public void execute(final Editor editor, DataContext dataContext) {
				if (editor.getSelectionModel().hasSelection()) {
					int selStart = editor.getSelectionModel().getSelectionStart();
					int selEnd = editor.getSelectionModel().getSelectionEnd();
					final VisualPosition rangeStart = editor.offsetToVisualPosition(Math.min(selStart, selEnd));
					final VisualPosition rangeEnd = editor.offsetToVisualPosition(Math.max(selStart, selEnd));
					new WriteCommandAction.Simple(editor.getProject()) {
						@Override
						protected void run() throws Throwable {
							final Pair<Integer,Integer> copiedRange =
									duplicateLinesRange(editor, editor.getDocument(), rangeStart, rangeEnd);
							if (copiedRange != null) {
								editor.getSelectionModel().setSelection(copiedRange.first, copiedRange.second);
							}
						}
					}.execute();
				}
				else {
					final VisualPosition caretPos = editor.getCaretModel().getVisualPosition();
					new WriteCommandAction.Simple(editor.getProject()) {
						@Override
						protected void run() throws Throwable {
							duplicateLinesRange(editor, editor.getDocument(), caretPos, caretPos);

						}
					}.execute();
				}
			}
		});
	}

	public static Pair<Integer, Integer> duplicateLinesRange(Editor editor, Document document, VisualPosition rangeStart, VisualPosition rangeEnd) {
		Pair<LogicalPosition, LogicalPosition> lines = EditorUtil.calcSurroundingRange(editor, rangeStart, rangeEnd);
		int offset = editor.getCaretModel().getOffset();

		LogicalPosition lineStart = lines.first;
		LogicalPosition nextLineStart = lines.second;
		int start = editor.logicalPositionToOffset(lineStart);
		int end = editor.logicalPositionToOffset(nextLineStart);
		if (end <= start) {
			return null;
		}
		String s = document.getCharsSequence().subSequence(start, end).toString();
		final int lineToCheck = nextLineStart.line - 1;

		int newOffset = end + offset - start;
		if(lineToCheck == document.getLineCount () /* empty document */
				|| lineStart.line == document.getLineCount() - 1 /* last line*/
				|| document.getLineSeparatorLength(lineToCheck) == 0)
		{
			s = "\n"+s;
			newOffset++;
		}
		document.insertString(end, s);

		editor.getCaretModel().moveToOffset(newOffset);
		editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
		return new Pair<Integer, Integer>(end, end+s.length()-1);   // don't include separator of last line in range to select

	}
}
