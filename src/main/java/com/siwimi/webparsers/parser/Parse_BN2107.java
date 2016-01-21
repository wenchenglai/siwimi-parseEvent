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

public class Parse_BN2107 implements Parser {
	
	@Override
	public List<Activity> getEvents(String eventsSourceUrl, String parser, LocationRepository locationRep, ActivityRepository activityRep) {

		// Step 1: Initialize States
		// Typically each parser has its unique data need.  For example, this is a book store's events.
		// Since a book store has the same address, so I created global variables here to store address data.
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
		// For performance reason, typically you select the whole area first using the parent container.
		// Then you use the high specificity selector (e.g. .class1.class2.class4) on the previously selected area.
		Element divRoot = doc.select("div.content").first();  // divRoot is the container area
		if (divRoot == null)
			return eventsOutput;
		
		Elements rows = divRoot.select("div.col-sm-12.col-md-12.col-lg-12.col-xs-12");
		
		// Step 4: Loop through the elements list - each look will add an event
		// Typically in here, every parser will have different code because each parser has different HTML markup
		// But the general guidelines are:
		// 1.  Always remember to find out the unique key that can identify this specific event.  We need this unique key so 
		//     when we run the same parser multiple times, there will be no duplicate events.
		//     Each parser needs to find its own unique key.  Some events pages have unique eventId, like this book store,
		//     but some event pages has to use combination of fields such as title + date.
		//     Each activity class has a String field call "customData".  You store your unique key in this customData field.
		// 2.  Always remember to find out if this event already exists in the database, before you do the full event parsing.  
		//     Do this early in the process.
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
			
			// eventId is guaranteed unique, so let's check it with the database.
			if (activityRep.isExisted(eventId, parser)) {
				continue;
			}
			
			// at this point, we've done our most of exceptional checking.
			// It's safe to parse and store the event now.
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

			// It's a good idea to create the event object and set the fields at the same place
			// So in future if we want to add new field or delete a field, we always know to locate
			// at the bottom of this getEvents function.
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
				
			// Every event must run this function after event object is created.
			// Basically it'll populate some necessary data such as coordinates and time zone.
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
