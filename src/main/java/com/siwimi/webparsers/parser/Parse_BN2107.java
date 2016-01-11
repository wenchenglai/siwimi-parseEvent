package com.siwimi.webparsers.parser;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.domain.Activity.Category;
import com.siwimi.webparsers.domain.Activity.LifeStage;
import com.siwimi.webparsers.repository.ActivityRepository;
import com.siwimi.webparsers.repository.LocationRepository;

public class Parse_BN2107 implements ParseWebsite {
	
	@Override
	public List<Activity> getEvents(String eventsSourceUrl, String parser, LocationRepository locationRep, ActivityRepository activityRep) {

		// Step 1: Initialize States
		List<Activity> eventsOutput = new ArrayList<Activity>();
		String defaultZipCode = "48104";
		String defaultAddress = "3235 Washtenaw Ave";
		String defaultEventHostUrl = "http://stores.barnesandnoble.com";
		String defaultEventImgUrl = "http://simg1.imagesbn.com/presources/storelocator/images/events/LrgIcon/SpecialEvents.png";
		
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
		
		// Step 3: Select Elements in list style
		Element divRoot = doc.select("div.content").first();
		if (divRoot == null)
			return eventsOutput;
		
		Elements rows = divRoot.select("div.col-sm-12.col-md-12.col-lg-12.col-xs-12");
		
		// Step 4: Loop through the elements list - add event
		for (Element e : rows) {
			if (e.children().size() < 2) {
				continue;
			}
			
			Element main = e.child(1);		
			Elements testElements = main.select("h3.lgTitles");
				
			if (testElements.size() != 1) {
				continue;
			}
			
			int errorCode = 0;
			Element link = main.child(0);
			String event_url = String.format("%1s%2s", defaultEventHostUrl, link.attr("href"));
			String eventId = getEventId(event_url);
			
			if (activityRep.isExisted(eventId)) {
				continue;
			}
			
			String title = link.child(0).text();
			
			Category category = defaultCategory;
			String possibleType = main.child(2).text();
			int fromAge = 0, toAge = 0;
			if (possibleType.contains("Storytime")) {
				category = Category.storytelling;
				fromAge = 1;
				toAge = 6;
			}
			
			// This page's dateTime location could change, depends on if this event has ageGroup specified
			Element dateTimeElement = main.child(6);
			
			String dateTime = null;
			if (dateTimeElement.tagName() == "span")
				dateTime = main.child(6).text();
			else
				dateTime = main.child(5).text();
			
			String description = main.text();
			
			String fromTime = null;
			try {
				fromTime = getFromTime(dateTime);
			} catch (Exception e2) {
				errorCode += ErrorCode.NoFromTime.getValue();
			}
			
			Date fromDate = null;
			try {
				fromDate = getFromDate(dateTime, fromTime);
			} catch (Exception e3) {
				errorCode += ErrorCode.NoFromDate.getValue();
			}
			
			Element imageRow = e.child(2);
			String imageUrl = defaultEventImgUrl;
			if (imageRow.children().size() == 2) {
				imageUrl = imageRow.child(0).child(0).attr("src").trim();
				
				if (imageUrl.substring(0, 4) != "http")
					imageUrl = String.format("http:%1s", imageUrl);
			}

			Activity newEvent = new Activity();
			newEvent.setIsDeletedRecord(false);		
			newEvent.setCreatedDate(new Date());
			newEvent.setCustomData(eventId);
			newEvent.setParser(parser);
			newEvent.setUrl(event_url);
			newEvent.setTitle(title);
			newEvent.setType(category);
			newEvent.setFromDate(fromDate);
			newEvent.setFromTime(fromTime);
			newEvent.setFromAge(fromAge);
			newEvent.setToAge(toAge);
			newEvent.setDescription(description);
			newEvent.setAddress(defaultAddress);
			newEvent.setZipCode(defaultZipCode);
            newEvent.setStage(LifeStage.Approved);
            errorCode += this.setImage(newEvent, imageUrl);
			newEvent.setErrorCode(errorCode);
				
			PostProcessing(newEvent, locationRep);
			
			eventsOutput.add(newEvent);
		}
		
		return eventsOutput;
	}
	
	private Date getFromDate(String dateTimeText, String fromTime) {
		String[] splitted = dateTimeText.replace(',', ' ').split("\\s+");
		SimpleDateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy hh:mm aaa");
		Date fromDate;
		try {
			fromDate = formatter.parse(String.format("%1s %2s, %3s %4s", splitted[1], splitted[2], splitted[3], fromTime == null ? defaultFromTime : fromTime));
		} catch (ParseException e) {
			fromDate = null;
			e.printStackTrace();
		}
		return fromDate;

	}
	
	private String getFromTime(String dateTimeText) {
		String[] splitted = dateTimeText.split("\\s+");
		return String.format("%1s %2s", splitted[4], splitted[5]);
	}

	private String getEventId(String url) {
		String[] splitted = url.split("/");
		return splitted[4];
	}
}
