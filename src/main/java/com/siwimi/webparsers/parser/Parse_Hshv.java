package com.siwimi.webparsers.parser;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.domain.Activity.Category;
import com.siwimi.webparsers.domain.Activity.LifeStage;
import com.siwimi.webparsers.repository.ActivityRepository;
import com.siwimi.webparsers.repository.LocationRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Parse_Hshv implements Parser {

    @Override
    public List<Activity> getEvents(String eventsSourceUrl, String parser, LocationRepository locationRep, ActivityRepository activityRep) {
        List<Activity> eventsOutput = new ArrayList<Activity>();

        /* Initialize States*/
        String defaultZipCode = "48105";
        String defaultCity = "Ann Arbor";
        String defaultState = "Michigan";
        String eventAddress = "3100 Cherry Hill Rd. Ann Arbor, MI";


        // add first date of this month to fetch event of this moth
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        SimpleDateFormat ft = new SimpleDateFormat("M,1,YYYY");
        String thisMonthFirstDate = ft.format(date);;
        String calendarDate = ft.format(date);
        eventsSourceUrl += "&Day="+calendarDate;


        String monthRegexRule = "(January|Jan|February|Feb|March|Mar|April|Apr|May|June|July|August|Aug|September|October|Oct|November|Nov|December)";
        Category category = Category.misc;

        // parse event index page
        Document doc = null;
        try {
            doc = Jsoup.connect(eventsSourceUrl).userAgent("chrome").get();
        } catch (Exception e) {
            doc = null;
            e.printStackTrace();
        }

        if (doc == null) {
            return eventsOutput;
        }

        // find event date
        Elements eventDateListElements = doc.select(".callist");
        for (Element eventDateListElement : eventDateListElements) {
            // fetch event date
            String eventDateStr = eventDateListElement.select(".datelink").first().text();
            eventDateStr = this.getRegexString(monthRegexRule + "\\s\\d{1,2}\\,\\s\\d{4}", "", eventDateStr);
            String dateFormat = "";
            if (!"".equals(eventDateStr)) {
                dateFormat = "MMMM dd,yyyy";
            }

            // fetch event of each date
            Elements eventDetailLinkElements = eventDateListElement.select("a.callistevent");
            for (Element eventDetailLinkElement : eventDetailLinkElements) {
                String eventDetailUrl = eventDetailLinkElement.attr("href");
                String eventId = this.getRegexString("id=\\d+", "id=", eventDetailUrl);

                // skip duplicate events
                if (activityRep.isExisted(eventId, parser)) {
                    continue;
                }

                // parse event detail page
                Document eventDetailDoc = null;
                try {
                    eventDetailDoc = Jsoup.connect(eventDetailUrl).userAgent("chrome").get();
                } catch (Exception e) {
                    eventDetailDoc = null;
                    e.printStackTrace();
                }

                if (eventDetailDoc == null) {
                    continue;
                }

                // init event data
                int errorCode = 0;
                String eventFromTime = defaultFromTime;
                String eventFromTimeFormat = "";
                String eventToTime = "";
                String eventToTimeFormat = "";
                String fromAgeStr = "";
                String toAgeStr = "";


                String eventTitle = eventDetailDoc.select(".eventtitle").text();
                // try second format
                if ("".equals(eventTitle)) {
                    eventTitle = eventDetailDoc.select("#lo-EventTitle").text();
                }


                String eventDescription = "";
                Elements eventDetailTable = eventDetailDoc.select(".appArea>table");
                if (eventDetailTable.size() > 0) {
                    eventDescription = eventDetailDoc.select(".appArea>table").get(1).text().replaceAll("", "");
                    eventDescription = eventDescription.replaceAll("2016 Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec", "").trim();

                    // find address
                    String eventAddressStr = this.getRegexString("Address:.+\\d{5}\\s(United States)?", "Address:", eventDescription);
                    if (!"".equals(eventAddressStr))
                        eventAddress = eventAddressStr;
                    String eventTimeStr = this.getRegexString("Time:\\s?\\d{1,2}:\\d{1,2}\\s?(AM|PM)\\s?-\\s?\\d{1,2}:\\d{1,2}\\s?(AM|PM)", "", eventDescription, false);
                    if (!"".equals(eventTimeStr)) {
                        eventFromTime = this.getRegexString("\\d{1,2}:\\d{1,2}\\s?(AM|PM)\\s?-", "-", eventTimeStr, false);
                        if (!"".equals(eventFromTime)) {
                            eventFromTimeFormat = "hh:mm aaa";
                        }

                        eventToTime = this.getRegexString("-\\s?\\d{1,2}:\\d{1,2}\\s?(AM|PM)\\s?", "-", eventTimeStr, false);
                        if (!"".equals(eventToTime)) {
                            eventToTimeFormat = "hh:mm aaa";
                        }
                    }

                    // fetch event age
                    String ageStr = this.getRegexString("Ages:\\s\\d-\\d", "", eventDescription, false);
                    if (!"".equals(ageStr)) {
                        ageStr = ageStr.replaceAll("(?i)Age:", "").trim();
                        fromAgeStr = this.getRegexString("\\d-", "-", ageStr, false);
                        toAgeStr = this.getRegexString("-\\d", "-", ageStr, false);
                    }
                } else {
                    // second format
                    eventDescription = eventDetailDoc.select("#lo-UserDetailEventDescription").text();
                    String eventTimeStr = eventDetailDoc.select("#lo-start_time").text();
                    eventTimeStr = this.getRegexString("\\s?\\d{1,2}:\\d{1,2}\\s?(AM|PM)\\s?-\\s?\\d{1,2}:\\d{1,2}\\s?(AM|PM)", "", eventTimeStr, false);

                    if (!"".equals(eventTimeStr)) {
                        eventFromTime = this.getRegexString("\\d{1,2}:\\d{1,2}\\s?(AM|PM)\\s?-", "-", eventTimeStr, false);
                        if (!"".equals(eventFromTime)) {
                            eventFromTimeFormat = "hh:mm aaa";
                        }

                        eventToTime = this.getRegexString("-\\s?\\d{1,2}:\\d{1,2}\\s?(AM|PM)\\s?", "-", eventTimeStr, false);
                        if (!"".equals(eventToTime)) {
                            eventToTimeFormat = "hh:mm aaa";
                        }
                    }
                }

                /* parse from date and to date*/
                Date eventFromDate = null;
                Date eventToDate = null;

                // parse from date
                if (!"".equals(eventDateStr) && !"".equals(dateFormat)) {
                    SimpleDateFormat formatter = new SimpleDateFormat(dateFormat + " " + eventFromTimeFormat);
                    try {
                        eventFromDate = formatter.parse(eventDateStr + " " + eventFromTime);
                    } catch (ParseException e) {
                        errorCode += ErrorCode.NoFromDate.getValue();
                        e.printStackTrace();
                    }
                }

                // parse to date
                if (!"".equals(eventDateStr) && !"".equals(eventToTimeFormat)) {
                    SimpleDateFormat formatter = new SimpleDateFormat(dateFormat + " " + eventToTimeFormat);
                    try {
                        eventToDate = formatter.parse(eventDateStr + " " + eventToTime);
                    } catch (ParseException e) {
                        errorCode += ErrorCode.NoFromDate.getValue();
                        e.printStackTrace();
                    }
                }

                /* store event*/
                Activity newEvent = new Activity();
                newEvent.setIsDeletedRecord(false);
                newEvent.setCreatedDate(new Date());
                newEvent.setParser(parser);
                newEvent.setTitle(eventTitle);
                newEvent.setDescription(eventDescription);
                newEvent.setZipCode(defaultZipCode);
                newEvent.setState(defaultState);
                newEvent.setCity(defaultCity);
                newEvent.setUrl(eventDetailUrl);
                newEvent.setType(category);
                newEvent.setAddress(eventAddress);
                newEvent.setFromDate(eventFromDate);
                newEvent.setFromTime(eventFromTime);
                newEvent.setCustomData(eventId);
                newEvent.setStage(LifeStage.Approved);
                newEvent.setErrorCode(errorCode);

                if (!"".equals(eventToTime)) {
                    newEvent.setToTime(eventToTime);
                }
                if (eventToDate == null) {
                    newEvent.setToDate(eventToDate);
                }
                if (!"".equals(fromAgeStr)) {
                    int fromAge = Integer.parseInt(fromAgeStr);
                    newEvent.setFromAge(fromAge);
                }
                if (!"".equals(toAgeStr)) {
                    int toAge = Integer.parseInt(toAgeStr);
                    newEvent.setToAge(toAge);
                }

                PostProcessing(newEvent, locationRep);
                this.checkEvent(newEvent);

                eventsOutput.add(newEvent);
            }
        }

        return eventsOutput;
    }

    private void checkEvent(Activity newEvent) {
        int errorCode = newEvent.getErrorCode();

        if ("".equals(newEvent.getTitle()))
            errorCode += ErrorCode.NoTitle.getValue();

        if ("".equals(newEvent.getDescription()))
            errorCode += ErrorCode.NoDescription.getValue();

        if ("".equals(newEvent.getFromTime())) {
            errorCode += ErrorCode.NoFromTime.getValue();
        }

        if (newEvent.getFromDate() == null) {
            errorCode += ErrorCode.NoFromDate.getValue();
        }

        newEvent.setErrorCode(errorCode);

    }
}