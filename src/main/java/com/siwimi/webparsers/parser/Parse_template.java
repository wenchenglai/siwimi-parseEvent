package com.siwimi.webparsers.parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.domain.Location;
import com.siwimi.webparsers.repository.ActivityRepository;
import com.siwimi.webparsers.repository.LocationRepository;

public class Parse_template implements ParseWebsite{

	@Override
	public List<Activity> getEvents(String eventsSourceUrl, String creator, LocationRepository locationRep) {
		List<Activity> activities = new ArrayList<Activity>();
		
		/* 
		 *   Implement parser here (by Jsoup or other libraries)
		 */
		
		return activities;
	}
	
	public void saveActivity(List<Activity> activities, ActivityRepository activityRep, LocationRepository locationRep) {
		if (activities != null) {
			for (Activity activity : activities) {
				activity = updateLocationAndTime(activity,locationRep);
				if (activityRep.queryExistedActivity(activity.getCreator(),activity.getTitle(),activity.getDescription()) == null)
					activityRep.saveActivity(activity);
			}
		}
	}
	
	public Activity updateLocationAndTime(Activity activity, LocationRepository locationRep) {
		// lookup location from the collection Location;
		Location thisLocation = locationRep.queryLocation(activity.getZipCode(), activity.getCity(), activity.getState());
		// set longitude and latitude 
		if (thisLocation!=null) {
			double[] location = {thisLocation.getLongitude(), thisLocation.getLatitude()};
			activity.setZipCode(thisLocation.getZipCode());
			activity.setLocation(location);
			activity.setCity(thisLocation.getTownship());
			activity.setState(thisLocation.getStateCode());
		}

		SimpleDateFormat formatter = new SimpleDateFormat("EEEE MMMM dd, yyyy hh:mm aaa");
	    String fromDateString = formatter.format(activity.getFromDate());	 
	    String toDateString = formatter.format(activity.getToDate());
	    if (thisLocation.getTimezone() != null) {
	    	if (thisLocation.getTimezone().contains("-5"))
	    		formatter.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    	else if (thisLocation.getTimezone().contains("-6"))
	    		formatter.setTimeZone(TimeZone.getTimeZone("America/Winnipeg"));
	    	else if (thisLocation.getTimezone().contains("-7"))
	    		formatter.setTimeZone(TimeZone.getTimeZone("America/Phoenix"));
	    	else if (thisLocation.getTimezone().contains("-8"))
	    		formatter.setTimeZone(TimeZone.getTimeZone("America/Vancouver"));		    	
	    } else {
	    	formatter.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    }	    
	   try {
		   activity.setFromDate(formatter.parse(fromDateString));
		   activity.setToDate(formatter.parse(toDateString));
	   } catch (ParseException e) {
		   e.printStackTrace();
	   }								
		return activity;
	}	
}
