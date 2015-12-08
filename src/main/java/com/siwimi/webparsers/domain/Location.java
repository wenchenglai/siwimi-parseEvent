package com.siwimi.webparsers.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Location")
public class Location {

	@Id
	private String id;
	
	private String countryCode;
	private String zipCode;
	private String township;
	private String state;
	private String stateCode;
	private String county;
	private String countyCode;
	
	private double longitude;
	private double latitude;
	
	private String timezone;
	private String dst;
	
	// This field is only for front-end purpose : User comments
	@Transient
	private Boolean dayLightSaving;
	
	public String getId() {
		return id;
	}	
	
	public String getCountryCode() {
		return countryCode;
	}
	
	public String getTownship() {
		return township;
	}
	
	public String getState() {
		return state;
	}
	
	public String getStateCode() {
		return stateCode;
	}
	
	public String getCounty() {
		return county;
	}
	
	public String getCountyCode() {
		return countyCode;
	}
	
	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}
	
	public String getZipCode() {
		if (zipCode != null) {
			if (Integer.parseInt(zipCode)<10000)
				zipCode = "0"+zipCode;	
		}
		return zipCode;
	}
	
	public String getTimezone() {
		return timezone;
	}

	public String getDst() {
		if (this.dst.equals("1"))
			this.dayLightSaving = true;
		else
			this.dayLightSaving = false;
		
		return dst;
	}

	public Boolean getDayLightSaving() {
		return this.dayLightSaving;
	}

	@Override
	public int hashCode() {
		if (township == null)
			return id.hashCode();
		else
			return township.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
	    if (obj == null)
	        return false;
	    if (getClass() != obj.getClass())
	        return false;
	    
	    if (this.getTownship() == null)
	    	return false;
	    if (this.getState() == null)
	    	return false;
	    if (this.getStateCode() == null)
	    	return false;

	    Location other = (Location) obj;
	    if (other.getTownship() == null)
	    	return false;
	    if (other.getState() == null)
	    	return false;
	    if (other.getStateCode() == null)
	    	return false;
	    
	    if (  !((this.getTownship().equals(other.getTownship())) && 
	    		(this.getState().equals(other.getState())) && 
	    		(this.getStateCode().equals(other.getStateCode())))  
	    		)
	       return false;
	    
	    return true;
	}
}
