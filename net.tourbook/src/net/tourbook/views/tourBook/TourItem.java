/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation version 2 of the License.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA    
 *******************************************************************************/
package net.tourbook.views.tourBook;

import java.util.ArrayList;

public class TourItem extends Object {

	static final int			ITEM_TYPE_ROOT			= 0;
	static final int			ITEM_TYPE_YEAR			= 20;
	static final int			ITEM_TYPE_MONTH			= 30;
	static final int			ITEM_TYPE_TOUR			= 40;

	private TourItem			parent					= null;
	private ArrayList<TourItem>	children				= null;

	long[]						fTourItemData;
	private long				dateValue;
	private int					fItemType;

	private static final int	CHILDREN_STATUS_NOT_SET	= 0;
	private static final int	CHILDREN_STATUS_IS_LEAF	= 1;
	private static final int	CHILDREN_STATUS_FETCHED	= 2;

	private int					childrenStatus			= CHILDREN_STATUS_NOT_SET;

	/**
	 * when the type for the tour item is <code>ITEM_TYPE_TOUR</code>, this
	 * field contains the tourId for the tour
	 */
	private long				fTourId;
	private long				fTourTypeId;

	TourItem(int itemType, long[] itemData, Long tourTypeId) {

		fItemType = itemType;
		fTourItemData = itemData;
		fTourTypeId = tourTypeId;

		// the date value can be year, month or day
		if ((itemType == ITEM_TYPE_YEAR || itemType == ITEM_TYPE_MONTH || itemType == ITEM_TYPE_TOUR)
				&& itemData.length > 0) {
			this.dateValue = itemData[0];
		}

		// get the tour id from the last column
		if (itemType == ITEM_TYPE_TOUR) {
			fTourId = itemData[itemData.length - 1];
		}

	}

	TourItem(int itemType, long[] itemData) {
		this.fItemType = itemType;
		this.fTourItemData = itemData;

		// the date value can be year, month or day
		if ((itemType == ITEM_TYPE_YEAR || itemType == ITEM_TYPE_MONTH || itemType == ITEM_TYPE_TOUR)
				&& itemData.length > 0) {
			this.dateValue = itemData[0];
		}

		// get the tour id from the last column
		if (itemType == ITEM_TYPE_TOUR) {
			fTourId = itemData[itemData.length - 1];
		}

		if (itemType == ITEM_TYPE_ROOT) {
			fTourTypeId = -99;
		}
	}

	public boolean hasChildren() {

		if (childrenStatus == CHILDREN_STATUS_NOT_SET) {
			/*
			 * if the children have not yet been retrieved we assume that
			 * children can be available to make the tree node expandable
			 */
			return true;

		} else if (childrenStatus == CHILDREN_STATUS_FETCHED) {
			return children.size() > 0;

		} else {
			return false;
		}

	}

	public TourItem[] getChildren() {
		if (children == null) {
			return new TourItem[0];
		}
		return children.toArray(new TourItem[children.size()]);
	}

	public ArrayList<TourItem> getChildrenList() {
		if (children == null) {
			return new ArrayList<TourItem>();
		}
		return children;
	}

	public TourItem getParent() {
		return parent;
	}

	void setParent(TourItem parent) {
		this.parent = parent;
	}

	public boolean hasChildrenBeenFetched() {

		return childrenStatus == CHILDREN_STATUS_FETCHED
				|| childrenStatus == CHILDREN_STATUS_IS_LEAF;
	}

	public int getItemType() {
		return fItemType;
	}

	public void setItemType(int type) {
		this.fItemType = type;
	}

	public void addChild(TourItem child) {
		if (children == null) {
			children = new ArrayList<TourItem>();
			childrenStatus = CHILDREN_STATUS_FETCHED;
		}
		children.add(child);
		child.setParent(this);
	}

	/**
	 * set this touritem to a leaf, this indicates no children are available
	 */
	public void setLeaf() {
		childrenStatus = CHILDREN_STATUS_IS_LEAF;
	}

	public long getTourId() {
		return fTourId;
	}

	/**
	 * @return Returns the dateValue.
	 */
	public long getDateValue() {
		return dateValue;
	}

	/**
	 * @return Returns the childrenStatus.
	 */
	public int getChildrenStatus() {
		return childrenStatus;
	}

	public long getTourTypeId() {
		return fTourTypeId;
	}
}
