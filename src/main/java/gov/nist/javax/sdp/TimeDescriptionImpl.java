package gov.nist.javax.sdp;

import java.util.Vector;

import javax.sdp.SdpException;
import javax.sdp.Time;
import javax.sdp.TimeDescription;

import gov.nist.javax.sdp.fields.RepeatField;
import gov.nist.javax.sdp.fields.TimeField;

/**
 * Implementation of Time Description
 */
public class TimeDescriptionImpl implements TimeDescription {
	private TimeField timeImpl;
	private Vector repeatList;

	public TimeDescriptionImpl() {
		timeImpl = new TimeField();
		repeatList = new Vector();

	}

	/**
	 * constructor
	 *
	 * @param timeField time field to create this description from
	 */
	public TimeDescriptionImpl(TimeField timeField) {
		this.timeImpl = timeField;
		repeatList = new Vector();
	}

	/**
	 * Returns the Time field.
	 *
	 * @return Time
	 */
	public Time getTime() {
		return timeImpl;
	}

	/**
	 * Sets the Time field.
	 *
	 * @param timeField Time to set
	 * @throws SdpException if the time is null
	 */
	@Override
	public void setTime(Time timeField) throws SdpException {
		if(timeField == null) {
			throw new SdpException("The parameter is null");
		}

		if(timeField instanceof TimeField) {
			this.timeImpl = (TimeField) timeField;
		} else {
			throw new SdpException("The parameter is not an instance of TimeField");
		}
	}

	/**
	 * Returns the list of repeat times (r= fields) specified in the
	 * SessionDescription.
	 *
	 * @param create boolean to set
	 * @return Vector
	 */
	public Vector getRepeatTimes(boolean create) {
		return this.repeatList;
	}

	/**
	 * Returns the list of repeat times (r= fields) specified in the
	 * SessionDescription.
	 *
	 * @param repeatTimes Vector to set
	 * @throws SdpException if the parameter is null
	 */
	public void setRepeatTimes(Vector repeatTimes) throws SdpException {
		this.repeatList = repeatTimes;
	}

	/**
	 * Add a repeat field.
	 *
	 * @param repeatField -- repeat field to add.
	 */
	public void addRepeatField(RepeatField repeatField) {
		if(repeatField == null) {
			throw new NullPointerException("null repeatField");
		}

		this.repeatList.add(repeatField);
	}

	@Override
	public String toString() {
		String retval = timeImpl.encode();

		for(int i = 0; i < this.repeatList.size(); i++) {
			RepeatField repeatField = (RepeatField) this.repeatList.elementAt(i);

			retval += repeatField.encode();
		}

		return retval;
	}
}
