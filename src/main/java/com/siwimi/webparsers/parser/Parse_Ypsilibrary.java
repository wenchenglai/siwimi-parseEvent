package com.siwimi.webparsers.parser;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.domain.Activity.Category;
import com.siwimi.webparsers.domain.Activity.LifeStage;
import com.siwimi.webparsers.repository.ActivityRepository;
import com.siwimi.webparsers.repository.LocationRepository;

public class Parse_Ypsilibrary implements Parser {
	
	@Override
	public List<Activity> getEvents(String eventsSourceUrl, String parser, LocationRepository locationRep, ActivityRepository activityRep) {
		
		/*
		 *	All of the event data from this website is contained in its Javascript variable. 
		 */
		
		// Step 1: Initialize States
		List<Activity> eventsOutput = new ArrayList<Activity>();

		// Step 2: Retrieve HTML Page
		Document doc = null;
		try {
			doc = Jsoup.connect(eventsSourceUrl).get();
		} catch (IOException e) {
			doc = null;
			e.printStackTrace();
		}				
		
		if (doc == null)
			return eventsOutput;
				
		// Step 3: Select iframe Element in list style
		Element iframe = doc.select("div#page_wrap")
				             .select("div#main_content")
				             .select("div#center_column")
				             .select("div#content")
				             .select("div#content-inner")
				             .select("div#content-area")
				             .select("iframe.overflow-y").first();  // divRoot is the container area
		String iframeSrc = iframe.attr("src");
		
		// Step 4: Retrieve nested HTML Page
		Document iframeContentDoc = null;
		if (iframeSrc != null)
			try {
				iframeContentDoc = Jsoup.connect("http:"+iframeSrc).get();
			} catch (IOException e) {
				iframeContentDoc = null;
				e.printStackTrace();
			}	
		if (iframeContentDoc == null)
			return eventsOutput;
		
		// Step 5: Select Element which contains cDATA in Javascript
		Element divRoot = iframeContentDoc.select("div#mcCon").select("script").first();		
		if (divRoot == null)
			return eventsOutput;
		
		// Step 6 : only select "var eventGridData" from the cDATA
		Matcher m = Pattern.compile("eventGridData = (.+?);").matcher(divRoot.html());
		List<String> s = new ArrayList<String>();
		while(m.find())
			s.add(m.group(1));	
		if (s.size()<1)
			return eventsOutput;		
		String eventData = "{\"eventData\":"+s.get(0)+"}";

		// Step 7 : parse eventData into JSON
		JSONArray allEvents = new JSONObject(eventData).getJSONArray("eventData");
		if (allEvents.length() < 1)
			return eventsOutput;
		
		for (int i = 0; i < allEvents.length(); i++)
		{
			JSONArray eventsOfDay = allEvents.getJSONObject(i).getJSONArray("Events");
			if (eventsOfDay.length() < 1)
				continue;
			
			for (int j = 0; j < eventsOfDay.length(); j++) {
				JSONObject event = eventsOfDay.getJSONObject(j);
				
				// Retrieve fields from eventData
				int id = event.optInt("EventId");
				String type = event.optString("EventType",null);
				String eventDateTime = event.getJSONObject("EventDateTime").optString("EventDateTime",null);
				int eventDuaration = event.getJSONObject("EventDateTime").optInt("EventDuaration");
				String location = event.getJSONObject("Location").optString("Name",null);
				String defaultEventHostUrl = "http://http://www.ypsilibrary.org/events";
				
				// eventDateTime+id is guaranteed unique, so let's check it with the database.
				if (activityRep.isExisted(eventDateTime+id, parser)) {
					continue;
				}
				
				int errorCode = 0;
				
				// Induce title
				String title = event.optString("Title",null);
				if (title == null) {
					errorCode += ErrorCode.NoTitle.getValue();
				}
				
				// Induce description
				String description = event.optString("Description",null);
				if (description == null) {
					errorCode += ErrorCode.NoDescription.getValue();
				}
				
				// Induce eventImageURL
				String imageUrl = event.optString("EventImageURL", null);
				if (imageUrl == null) {
					errorCode += ErrorCode.NoImageUrl.getValue();
				}
								
				// Induce category, fromAge, and toAge
				Category category = defaultCategory;
				int fromAge = 0, toAge = 99;
				if (type.toLowerCase().contains("story")) {
					category = Category.storytelling;
					fromAge = 1;
					toAge = 6;
				} else if (type.toLowerCase().contains("film")) {
					category = Category.movie;				
				} else if (type.toLowerCase().contains("art")) {
					category = Category.art;				
				} else if (type.toLowerCase().contains("science")) {
					category = Category.science;				
				} else if (type.toLowerCase().contains("game")) {
					category = Category.game;				
				} 
				
				// Induce fromDate
				Date fromDate = null;
				if (eventDateTime == null) {
					errorCode += ErrorCode.NoFromDate.getValue();
				} else {
					try {
						fromDate = getFromDate(eventDateTime);
					} catch (Exception e3) {
						errorCode += ErrorCode.NoFromDate.getValue();
					}
				}

				// Induce fromTime
				String fromTime = null;
				if (fromDate == null) {
					errorCode += ErrorCode.NoFromTime.getValue();
				} else {
					try {
						fromTime = getHM(fromDate);
					} catch (Exception e2) {
						errorCode += ErrorCode.NoFromTime.getValue();
					}					
				}

				// Induce toDate
				Date toDate = null;
				if ((fromDate!=null) && (eventDuaration > 0)) {
					toDate = getToDate(fromDate,eventDuaration);
				} 
				
				// Induce toTime
				String toTime = null;
				if (toDate != null) {
					toTime = getHM(toDate);
				}
				
				// Induce address
				String address = "Ypsilanti district library";
				String zipcode = "48197";
				if (location != null) {
					if (location.contains("Whittaker")) {
						address = "5577 Whittaker Road (" + location + ")";
					} else if (location.contains("Michigan")) {
						address = "229 West Michigan Avenue (" + location + ")";				
					} else if (location.contains("Superior")) {
						address = "8795 MacArthur Blvd (" + location + ")";
						zipcode = "48198";					
					} else if (location.contains("Bookmobile")) {
						address = "229 West Michigan Avenue (" + location + ")";				
					} 	
				}
				
				// It's a good idea to create the event object and set the fields at the same place
				// So in future if we want to add new field or delete a field, we always know to locate
				// at the bottom of this getEvents function.
				Activity newEvent = new Activity();
				newEvent.setIsDeletedRecord(false);		
				newEvent.setCreatedDate(new Date());
				newEvent.setCustomData(eventDateTime+id);
				newEvent.setParser(parser);
				newEvent.setUrl(defaultEventHostUrl);
				newEvent.setTitle(title);
				newEvent.setType(category);
				newEvent.setFromDate(fromDate);
				newEvent.setFromTime(fromTime);
				newEvent.setToDate(toDate);
				newEvent.setToTime(toTime);
				newEvent.setFromAge(fromAge);
				newEvent.setToAge(toAge);
				newEvent.setDescription(description);
				newEvent.setAddress(address);
				newEvent.setZipCode(zipcode);
	            newEvent.setStage(LifeStage.Approved);
	            errorCode += this.setImage(newEvent, imageUrl);
				newEvent.setErrorCode(errorCode);
					
				// Every event must run this function after event object is created.
				// Basically it'll populate some necessary data such as coordinates and time zone.
				PostProcessing(newEvent, locationRep);				
				eventsOutput.add(newEvent);				
			}
		}
		return eventsOutput;
	}
	
	private Date getFromDate(String fromDateString) {
		Date fromDate = null;
		SimpleDateFormat isoFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aaa");
		isoFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		   try {
			fromDate = isoFormat.parse(fromDateString);
		   } catch (ParseException e) {
			   e.printStackTrace();
		   }
		return fromDate;		
	}
	
	private Date getToDate(Date fromDate, int duration) {
		Calendar c = Calendar.getInstance();
		c.setTime(fromDate);
		c.add(Calendar.MINUTE, duration);
		return c.getTime();
	}
	
	private String getHM (Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		int hour = c.get(Calendar.HOUR);
		int minute = c.get(Calendar.MINUTE);
		int am_pm = c.get(Calendar.AM_PM);
		
		//The format string %02d formats an integer with leading zeros 
		//(that's the 0 in %02d), always taking up 2 digits of width
		String time = String.format("%02d:%02d", hour, minute);;
		switch (am_pm) {
	    	case Calendar.AM:
	    		time += " AM";
	        	break;
	    	default:
	    		time += " PM";
	    		break;
		}
		
		return time;
	}
}
