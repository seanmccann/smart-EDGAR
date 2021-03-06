package ch.pschatzmann.edgar.base;

import java.io.Serializable;

/**
 * Segment information in XBRL file
 */

public class Segment implements Serializable, Comparable<Segment> {
	private String id;
	private String description;
	private String dimension;
	private String dimensionDescription;

	public Segment(String id, String description, String dimension, String dimensionDescription) {
		this.id = id;
		this.description = description;
		this.dimension = dimension;
		this.dimensionDescription = dimensionDescription;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDimension() {
		return dimension;
	}

	public void setDimension(String dimension) {
		this.dimension = dimension;
	}

	public String getDimensionDescription() {
		return dimensionDescription;
	}

	public void setDimensionDescription(String dimensionDedscription) {
		this.dimensionDescription = dimensionDedscription;
	}

	public String toString() {
		return description;
	}

	@Override
	public int compareTo(Segment o) {
		return this.getDescription().compareTo(o.getDescription());
	}

}
