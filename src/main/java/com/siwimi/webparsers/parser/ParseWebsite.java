package com.siwimi.webparsers.parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.domain.Location;
import com.siwimi.webparsers.repository.ActivityRepository;
import com.siwimi.webparsers.repository.LocationRepository;

public interface ParseWebsite {
	enum ErrorCode {
		NoError(0),
		NoFromDate(1),
		NoFromTime(2),
		NoTitle(4),
		NoDescription(8),
		NoImageUrl(16),
		NoImageBase64(32);
	
		private final int value;
		
		ErrorCode(int value) {
			this.value = value;
		}
		
	    public int getValue() {
	        return value;
	    }
	};
	
	// NOTE: we put shared variables here by all parsers
	String defaultFromTime = "12:00 am";
    String defaultCategory = "misc";
	
	// NOTE: added locationRep, remove it from saveActivity, because saveActivity should be responsible for one thing only
	List<Activity> getEvents(String url, String creator, LocationRepository locationRep, ActivityRepository activityRep);
	
	default int saveEvents(List<Activity> activities, ActivityRepository activityRep) {		
		// NOTE: avoid nested code, also good for input validation.
		if (activities == null)
			return 0;
		
		int totalEventSaved = 0;
		for (Activity activity : activities) {
			if (activityRep.saveActivity(activity) != null);
				totalEventSaved += 1;
		}
		return totalEventSaved;
	};
	
	// NOTE: http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html#format
	/**
	 * Update the event with correct location Information.
	 * Location Information includes longitude, latitude, missing city, state or zip code.
	 * <p>
	 * This method will ONLY update the field that's missing.
	 *
	 * @param  event	an event that needs location information updated by system
	 * @param  location	Location object that has all the location information for this event
	 */
	default void updateEventLocation(Activity event, Location location) {
		double[] coordinates = {location.getLongitude(), location.getLatitude()};
		event.setLocation(coordinates);
		
		if (isNothing(event.getZipCode()))
			event.setZipCode(location.getZipCode());
		
		if (isNothing(event.getCity()))
			event.setCity(location.getTownship());
		
		if (isNothing(event.getState()))
			event.setState(location.getStateCode());
	}	
	
	default void updateEventTimeZone(Activity event, Location location) {
	    if (location.getTimezone() == null)
	    	return;
		
		SimpleDateFormat formatter = new SimpleDateFormat("EEEE MMMM dd, yyyy hh:mm aaa");
	    formatter.setTimeZone(getTimeZone(location.getTimezone()));
	    
	    // Now, we need to update the time with respect to the time zone.
	    String fromDateOriginal = null;
	    if (event.getFromDate() != null)
	    	fromDateOriginal = formatter.format(event.getFromDate());
	    
	    String toDateOriginal = null;
	    if (event.getToDate() != null)
	    	toDateOriginal = formatter.format(event.getToDate());
	    
	    try {
	    	if (fromDateOriginal != null)
	    		event.setFromDate(formatter.parse(fromDateOriginal));
		   
	    	if (toDateOriginal != null)
	    		event.setToDate(formatter.parse(toDateOriginal));
	    } catch (ParseException e) {
	    	e.printStackTrace();
	    }								
	}
	
	default String getImageBase64(String imageUrl) {		
		Response resultImageResponse = null;
		try {
			resultImageResponse = Jsoup.connect(imageUrl).ignoreContentType(true).execute();
		} catch (Exception e) {
			System.out.println(String.format("Siwimi Exception: Cannot download image data with url = %1s", imageUrl));
			e.printStackTrace();
			return null;
		}
		
		if (resultImageResponse == null)
			return null;
		
		Encoder encoder = Base64.getEncoder();
		String imageData = encoder.encodeToString(resultImageResponse.bodyAsBytes());
		
		String imageType = "jpeg";
		if (imageUrl.contains(".gif")) {
			imageType = "gif";
		} else if (imageUrl.contains(".png")) {
			imageType = "png";	
		}
		
		return String.format("data:image/%1s;base64,%2s", imageType, imageData);
	}
	
	default TimeZone getTimeZone(String input) {
		String zone = "America/New_York";
		
    	if (input.contains("-5"))
    		zone = "America/New_York";
    	else if (input.contains("-6"))
    		zone = "America/Winnipeg";
    	else if (input.contains("-7"))
    		zone = "America/Phoenix";
    	else if (input.contains("-8"))
    		zone = "America/Vancouver";
    	
    	return TimeZone.getTimeZone(zone);
	}
	
	default Boolean isNothing(String input) {
		return input == null || input.isEmpty() ? true : false;
	}

	/**
	 * update  location and time zone
	 * @param newEvent
	 * @param locationRep
	 */
	default void PostProcessing(Activity newEvent, LocationRepository locationRep) {
		Location location = locationRep.queryLocation(newEvent.getZipCode(), newEvent.getCity(), newEvent.getState());
		updateEventLocation(newEvent, location);
		updateEventTimeZone(newEvent, location);
	}

	/**
	 *
	 * @param pattern rule of regular expression
	 * @param filter needed to be filtered from result
	 * @param text source text
	 * @return
	 */
	default String getRegexString(String pattern,String filter ,String text){
		String result = "";

		Pattern rulePatten = Pattern.compile(pattern);
		Matcher patternMatcher = rulePatten.matcher(text);
		if (patternMatcher.find()) {
			result = patternMatcher.group(0).replace(filter,"").trim();
		}

		return result;
	}
}
