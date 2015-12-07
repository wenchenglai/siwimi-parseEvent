package com.siwimi.webapi.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="Activity")
@CompoundIndexes({
    @CompoundIndex(name = "title_createdDate", def = "{'title': 1, 'createdDate': -1}"),
    @CompoundIndex(name = "type_createdDate", def = "{'type': 1, 'createdDate': -1}"),
    @CompoundIndex(name = "fromDate_createdDate", def = "{'fromDate': 1, 'createdDate': 1}")
})
public class Activity {

	@Id
	private String id;
	
	private String creator;
	private String title;
	private String description;
	private Date fromDate;
	private Date toDate;	
	private String fromTime; 
	private String toTime;	
	private String address;
	private String city;
	private String state;
	private String zipCode;
	private String url;
	private String imageData;
	private String imageUrl;
	private String type;
	private int like;
	private int viewCount;
	private double[] location;
	private int fromAge;
	private int toAge;
	private float price;	
	private Date createdDate;
	
	// User comments
	@Transient
	private List<String> replies = new ArrayList<String>();	
	
	//This field is only for front-end purpose
	@Transient
	private String status = "upcoming";
	
	//This field is only for front-end purpose : total record count
	@Transient
	private int queryCount = 0;
	
	@Transient
	private boolean isFavorite = false;
	
	//The default of the below field is set by backend
	private Boolean isDeletedRecord = false;
	
	public String getFromTime() {
		return fromTime;
	}

	public void setFromTime(String fromTime) {
		this.fromTime = fromTime;
	}

	public String getToTime() {
		return toTime;
	}

	public void setToTime(String toTime) {
		this.toTime = toTime;
	}

	public int getFromAge() {
		return fromAge;
	}

	public void setFromAge(int fromAge) {
		this.fromAge = fromAge;
	}

	public int getToAge() {
		return toAge;
	}

	public void setToAge(int toAge) {
		this.toAge = toAge;
	}

	public float getPrice() {
		return price;
	}

	public void setPrice(float price) {
		this.price = price;
	}

	public Activity() {
		// this.status : this is for front-end only
		if (this.fromDate != null) {
			Date today = new Date();
			if (today.after(this.fromDate))
				this.status = "past";						
		} 
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public Date getFromDate() {
		return fromDate;
	}

	public void setFromDate(Date fromDate) {
/*		if (fromDate != null) {
			// Set HH:MM:SS = 03:15:00
			Calendar cal = Calendar.getInstance();
			cal.setTime(fromDate);
			cal.set(Calendar.HOUR_OF_DAY, 3);
			cal.set(Calendar.MINUTE, 15);
			cal.set(Calendar.SECOND, 0);
					
			this.fromDate = cal.getTime();	
		}*/
		this.fromDate = fromDate;
	}

	public Date getToDate() {
		return toDate;
	}

	public void setToDate(Date toDate) {
/*		if (toDate != null) {
			// Set HH:MM:SS = 20:45:59
			Calendar cal = Calendar.getInstance();
			cal.setTime(toDate);
			cal.set(Calendar.HOUR_OF_DAY, 20);
			cal.set(Calendar.MINUTE, 45);
			cal.set(Calendar.SECOND, 59);

			this.toDate = cal.getTime();	
		}*/
		this.toDate = toDate;
	}

	public String getImageData() {
		return imageData;
	}

	public void setImageData(String imageData) {
		this.imageData = imageData;
	}

	public String getImageUrl() {
		return imageUrl;
	}
	
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public double[] getLocation() {
		return location;
	}

	public void setLocation(double[] location) {
		this.location = location;
	}

	public String getZipCode() {
		return zipCode;
	}

	// ZipCode stored in MongoDB could only have 4-digit
	public void setZipCode(String zipCode) {
		if (zipCode != null) {
			if (Integer.parseInt(zipCode)<10000)
				zipCode = "0"+zipCode;	
		}
		this.zipCode = zipCode;
	}

	public int getLike() {
		return like;
	}

	public void setLike(int like) {
		this.like = like;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getViewCount() {
		return viewCount;
	}

	public void setViewCount(int viewCount) {
		this.viewCount = viewCount;
	}
	
	public boolean getIsFavorite() {
		return isFavorite;
	}

	public void setIsFavorite(boolean isFavorite) {
		this.isFavorite = isFavorite;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}	
	
	public Boolean getIsDeletedRecord() {
		return isDeletedRecord;
	}

	public void setIsDeletedRecord(Boolean isDeletedRecord) {
		this.isDeletedRecord = isDeletedRecord;
	}
	
	public int getQueryCount() {
		return queryCount;
	}

	public void setQueryCount(int queryCount) {
		this.queryCount = queryCount;
	}

	public List<String> getReplies() {
		return replies;
	}

	public void setReplies(List<String> Replies) {
		this.replies = Replies;
	}
	
	@Override
	public int hashCode() {
	    return id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
	    if (obj == null)
	        return false;
	    if (getClass() != obj.getClass())
	        return false;
	    
	    Activity other = (Activity) obj;
	    if (!this.getId().equals(other.getId()))
	          return false;
	       
	    return true;
	}

}
