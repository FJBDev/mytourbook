/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

package net.tourbook.data;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

@Entity
public class TourPerson implements Comparable<Object> {

	public static final int	DB_LENGTH_LAST_NAME			= 80;
	public static final int	DB_LENGTH_FIRST_NAME		= 80;
	public static final int	DB_LENGTH_RAW_DATA_PATH		= 255;
	public static final int	DB_LENGTH_DEVICE_READER_ID	= 255;

	public static final int	PERSON_ID_NOT_DEFINED		= -1;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long			personId					= PERSON_ID_NOT_DEFINED;

	@Basic(optional = false)
	private String			firstName;

	private String			lastName;

	private float			weight;

	private float			height;

	/**
	 * Date/Time when tour data was modified, default value is 0
	 * <p>
	 * since: db version 15
	 */
	private long			birthDay;

	/**
	 * Device used by this person, reference to the device plugin
	 */
	private String			deviceReaderId;

	/**
	 * path where the raw tour data will be saved after import
	 */
	private String			rawDataPath;

	/**
	 * default bike being used by this person
	 */
	@ManyToOne
	private TourBike		tourBike;

	/**
	 * unique id for manually created person because the {@link #personId} is
	 * {@value #PERSON_ID_NOT_DEFINED} when it's not persisted
	 */
	@Transient
	private long			_createId					= 0;

	/**
	 * manually created person creates a unique id to identify it, saved person is compared with the
	 * person id
	 */
	private static int		_createCounter				= 0;

	/**
	 * default constructor used in ejb
	 */
	public TourPerson() {}

	public TourPerson(final String firstName, final String lastName) {

		_createId = ++_createCounter;

		this.firstName = firstName;
		this.lastName = lastName;
	}

	@Override
	public int compareTo(final Object o) {

		// compare by last + first name

		if (o instanceof TourPerson) {

			final TourPerson otherPerson = (TourPerson) o;

			final int compareLastName = lastName.compareTo(otherPerson.getLastName());

			if (compareLastName != 0) {
				return compareLastName;
			}

			return firstName.compareTo(otherPerson.getFirstName());

		}

		return 0;
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TourPerson)) {
			return false;
		}

		final TourPerson other = (TourPerson) obj;

		if (_createId == 0) {

			// person is from the database
			if (personId != other.personId) {
				return false;
			}
		} else {

			// person was create
			if (_createId != other._createId) {
				return false;
			}
		}

		return true;
	}

	public long getBirthDay() {
		return birthDay;
	}

	public String getDeviceReaderId() {
		return deviceReaderId;
	}

	public String getFirstName() {
		return firstName;
	}

	public float getHeight() {
		return height;
	}

	public String getLastName() {
		return lastName;
	}

	/**
	 * @return Return the person first and last name
	 */
	public String getName() {
		return firstName + //
				(lastName.equals(UI.EMPTY_STRING) ? //
						UI.EMPTY_STRING
						: UI.SPACE + lastName);
	}

	public long getPersonId() {
		return personId;
	}

	public String getRawDataPath() {
		return rawDataPath;
	}

	public TourBike getTourBike() {
		return tourBike;
	}

	public float getWeight() {
		return weight;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_createId ^ (_createId >>> 32));
		result = prime * result + (int) (personId ^ (personId >>> 32));
		return result;
	}

	public boolean persist() {

		boolean isSaved = false;

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final EntityTransaction ts = em.getTransaction();

		try {

			if (getPersonId() == PERSON_ID_NOT_DEFINED) {
				// entity is new
				ts.begin();
				em.persist(this);
				ts.commit();
			} else {
				// update entity
				ts.begin();
				em.merge(this);
				ts.commit();
			}

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				isSaved = true;
			}
			em.close();
		}

		if (isSaved) {
			PersonManager.refreshPeople();
		}

		return isSaved;
	}

	public void setBirthDay(final long birthDay) {
		this.birthDay = birthDay;
	}

	public void setDeviceReaderId(final String deviceId) {
		this.deviceReaderId = deviceId;
	}

	public void setFirstName(final String name) {
		this.firstName = name;
	}

	public void setHeight(final float height) {
		this.height = height;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public void setRawDataPath(final String rawDataPath) {
		this.rawDataPath = rawDataPath;
	}

	public void setTourBike(final TourBike tourBike) {
		this.tourBike = tourBike;
	}

	public void setWeight(final float weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		return "TourPerson [personId=" + personId + ", firstName=" + firstName + ", lastName=" + lastName + "]";
	}
}
