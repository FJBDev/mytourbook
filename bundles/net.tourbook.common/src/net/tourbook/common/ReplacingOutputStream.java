/**
 * Source: http://stackoverflow.com/questions/1624830/java-search-replace-in-a-stream<br>
 * Date: 2015-01-28
 */
package net.tourbook.common;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;

/**
 * I got some good ideas from the link provided and ended up writing a small class to handle
 * replacement of $VAR$ variables in a stream.
 */
public class ReplacingOutputStream extends OutputStream {

	private static final int			DOLLAR_SIGN		= "$".codePointAt(0);		//$NON-NLS-1$
	private static final int			BACKSLASH		= "\\".codePointAt(0);		//$NON-NLS-1$

	private final OutputStream			delegate;
	private final Map<String, Object>	replacementValues;

	private int							previous		= Integer.MIN_VALUE;
	private boolean						_isReplacing	= false;

	private ArrayList<Integer>			replacement		= new ArrayList<Integer>();

	public ReplacingOutputStream(final OutputStream delegate, final Map<String, Object> replacementValues) {

		this.delegate = delegate;
		this.replacementValues = replacementValues;
	}

	private void doReplacement() throws IOException {

		final StringBuilder sb = new StringBuilder();
		for (final Integer intval : replacement) {
			final char[] chars = Character.toChars(intval);
			sb.append(chars);
		}

		replacement.clear();

		final String oldValue = sb.toString();
		final Object replaceValue = replacementValues.get(oldValue);

		if (replaceValue == null) {
			throw new RuntimeException("Could not find replacement variable for value '" + oldValue + "'."); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (replaceValue instanceof byte[]) {

			final byte[] newValue = (byte[]) replaceValue;

			delegate.write(newValue);

		} else {

			final String newValue = replaceValue.toString();

			for (int i = 0; i < newValue.length(); ++i) {

				final int value = newValue.codePointAt(i);

				delegate.write(value);
			}
		}
	}

	public @Override void write(final int b) throws IOException {

		if (b == DOLLAR_SIGN && previous != BACKSLASH) {

			if (_isReplacing) {
				doReplacement();
				_isReplacing = false;
			} else {
				_isReplacing = true;
			}

		} else {

			if (_isReplacing) {
				replacement.add(b);
			} else {
				delegate.write(b);
			}
		}

		previous = b;

	}
}
