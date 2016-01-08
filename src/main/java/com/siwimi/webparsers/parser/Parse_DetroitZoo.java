package com.siwimi.webparsers.parser;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.repository.ActivityRepository;
import com.siwimi.webparsers.repository.LocationRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Parse_DetroitZoo implements ParseWebsite {

    @Override
    public List<Activity> getEvents(String eventsSourceUrl, String parser, LocationRepository locationRep, ActivityRepository activityRep) {
        List<Activity> eventsOutput = new ArrayList<Activity>();

        /* Initialize States*/
        String defaultZipCode = "48067";
        String defaultCity = "Royal Oak";
        String defaultState = "Michigan";
        String defaultAddress = "8450 W. 10 Mile Road, Royal Oak, MI ";
        String monthRegexRule = "(January|February|March|April|May|June|July|August|September|October|November|December)";
        String category = "Zoo";

        // parse event index page
        Document doc = null;
        try {
            doc = Jsoup.connect(eventsSourceUrl).userAgent("chrome").get();
        } catch (Exception e) {
            doc = null;
            e.printStackTrace();
        }

        if (doc == null)
            return eventsOutput;

        Elements events = doc.select(".shuffle-item");
        for (Element eachEvent : events) {
            int errorCode = 0;
            Activity newEvent = new Activity();
            Document eventDetail = null;
            String eventDetailUrl = eachEvent.select("a").attr("href");
            // can't fetch detail page
            if ("".equals(eventDetailUrl)) {
                continue;
            }
            // do not fetch ended event
            if ("[\"past\"]".equals(eachEvent.attr("data-groups").trim())) {
                continue;
            }
            // fetch event detail page for more data
            try {
                eventDetail = Jsoup.connect(eventDetailUrl).userAgent("chrome").get();
            } catch (Exception e) {
                eventDetail = null;
                e.printStackTrace();
            }
            if (eventDetail == null) {
                continue;
            }

            String eventTitle = eventDetail.select(".event-title").text().trim();
            if ("".equals(eventTitle)) {
                errorCode += ErrorCode.NoTitle.getValue();
            }
            String eventDescription = eventDetail.select(".page-copy").text().trim();
            if ("".equals(eventDescription)) {
                errorCode += ErrorCode.NoDescription.getValue();
            }

            // fetch date and time
            String eventDate = eventDetail.select(".event-date").text().trim().replace(" ", "");
            String eventTime = eventDetail.select(".event-time").text().trim().replace(" ", "");

            // start to parse date and time
            Date eventFromDate = null;
            String eventFromDateStr = "";
            String eventFromTime = "";
            if (!"".equals(eventDate)) {
                // regularize data
                if (!eventDate.contains("January")) {
                    eventDate = eventDate.replace("Jan", "January");
                }
                if (!eventDate.contains("November")) {
                    eventDate = eventDate.replace("Nov", "November");
                }
                // parse format January12
                eventFromDateStr = getRegexString(monthRegexRule + "\\.?\\d+", "", eventDate);

                // this year
                Calendar now = Calendar.getInstance();
                int thisYear = now.get(Calendar.YEAR);
                // successfully fetch date and combine with year sting.
                if (!"".equals(eventFromDateStr)) {
                    eventFromDateStr = eventFromDateStr.replaceAll("\\.", "");
                    eventFromDateStr += " " + thisYear + " ";
                } else {
                    errorCode += ErrorCode.NoFromDate.getValue();
                }
            } else {
                errorCode += ErrorCode.NoFromDate.getValue();
            }

            // parse from time
            if (!"".equals(eventTime)) {
                // regularize data
                eventTime = eventTime.replace("to", "-");

                // try formate 11a.m.–3 p.m.
                eventFromTime = getRegexString("^\\d{1,2}(a|p)\\.m\\.", "-", eventTime);
                // fetch a.m or p.m => am pm
                String periodStr = getRegexString("(a|p)\\.m\\.$", "", eventTime).replaceAll("\\.", "");
                if (!"".equals(eventFromTime) && !"".equals(eventFromDateStr)) {
                    // fetch 11a.m
                    // strip .  11a.m => 11am
                    eventFromTime = eventFromTime.replaceAll("\\.", "");
                    SimpleDateFormat formatter = new SimpleDateFormat("MMMMdd yyyy hhaaa");
                    try {
                        eventFromDate = formatter.parse(eventFromDateStr + eventFromTime);
                    } catch (ParseException e) {
                        errorCode += ErrorCode.NoFromDate.getValue();
                        e.printStackTrace();
                    }
                } else {
                    // try formate 6:30-10 p.m.
                    eventFromTime = getRegexString("^\\d{1,2}:\\d{1,2}", "", eventTime);
                }

                if (eventFromDate == null) {
                    if (!"".equals(eventFromTime) && !"".equals(eventFromDateStr) && !"".equals(periodStr)) {
                                    /*format 6:30*/
                        SimpleDateFormat formatter = new SimpleDateFormat("MMMMdd yyyy hh:mm aaa");
                        try {
                            eventFromDate = formatter.parse(eventFromDateStr + eventFromTime + " " + periodStr);
                        } catch (ParseException e) {
                            errorCode += ErrorCode.NoFromDate.getValue();
                            e.printStackTrace();
                        }
                    } else {
                        // try formate 6-10:30 p.m.
                        eventFromTime = getRegexString("^\\d{1,2}-", "-", eventTime);
                    }
                }

                if (eventFromDate == null) {
                    if (!"".equals(eventFromTime) && !"".equals(eventFromDateStr) && !"".equals(periodStr)) {
                        /*format 6:30*/
                        SimpleDateFormat formatter = new SimpleDateFormat("MMMMdd yyyy hh aaa");
                        try {
                            eventFromDate = formatter.parse(eventFromDateStr + eventFromTime + " " + periodStr);
                        } catch (ParseException e) {
                            errorCode += ErrorCode.NoFromDate.getValue();
                            e.printStackTrace();
                        }
                    }
                }
            }

            // can not get from time
            if (eventFromDate == null && "".equals(eventFromTime) && !"".equals(eventFromDateStr)) {
                eventFromTime = defaultFromTime;
                /*format 6:30*/
                SimpleDateFormat formatter = new SimpleDateFormat("MMMMdd yyyy hh:mm aaa");
                try {
                    eventFromDate = formatter.parse(eventFromDateStr + defaultFromTime);
                } catch (ParseException e) {
                    errorCode += ErrorCode.NoFromDate.getValue();
                    e.printStackTrace();
                }
                errorCode += ErrorCode.NoFromTime.getValue();
            }


            // store event
            newEvent.setParser(parser);
            newEvent.setCreatedDate(new Date());
            newEvent.setTitle(eventTitle);
            newEvent.setDescription(eventDescription);
            newEvent.setZipCode(defaultZipCode);
            newEvent.setState(defaultState);
            newEvent.setCity(defaultCity);
            newEvent.setUrl(eventDetailUrl);
            newEvent.setType(category);
            newEvent.setAddress(defaultAddress);
            newEvent.setFromDate(eventFromDate);
            newEvent.setFromTime(eventFromTime);
            newEvent.setCustomData(eventDetailUrl);

            // fetch image
            String imageUrl = eachEvent.select("img").attr("src");
            errorCode += this.setImage(newEvent, imageUrl);

            newEvent.setErrorCode(errorCode);
            PostProcessing(newEvent, locationRep);

            eventsOutput.add(newEvent);
        }

        return eventsOutput;

    }
}