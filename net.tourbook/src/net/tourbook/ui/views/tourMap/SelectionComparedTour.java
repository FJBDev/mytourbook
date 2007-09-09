package net.tourbook.ui.views.tourMap;

import org.eclipse.jface.viewers.ISelection;

/**
 * Selection is a reference tour
 */
public class SelectionComparedTour implements ISelection {

	/**
	 * Id of the reference tour
	 */
	private Long	fRefId;

	private Long	fCompTourId;
	private Long	fCompareId;
	private int		fCompareStartIndex;
	private int		fCompareEndIndex;

	public SelectionComparedTour(Long refId) {
		fRefId = refId;
	}

	public int getCompareEndIndex() {
		return fCompareEndIndex;
	}

	public Long getCompareId() {
		return fCompareId;
	}

	public int getCompareStartIndex() {
		return fCompareStartIndex;
	}

	public Long getCompTourId() {
		return fCompTourId;
	}

	public Long getRefId() {
		return fRefId;
	}

	public boolean isEmpty() {
		return false;
	}

	/**
	 * Set data for the compared tour
	 * 
	 * @param compareId
	 *        database Id for the compared tour
	 * @param compTourId
	 *        database Id for the compared tour data
	 * @param compStartIndex
	 *        start index of the x-marker
	 * @param compEndIndex
	 *        end index of the x-marker
	 */
	public void setTourCompareData(	long compareId,
									long compTourId,
									int compStartIndex,
									int compEndIndex) {

		fCompareId = compareId;
		fCompTourId = compTourId;
		fCompareStartIndex = compStartIndex;
		fCompareEndIndex = compEndIndex;
	}

}
