package com.siwimi.webparsers.parser;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.domain.Activity.Category;
import com.siwimi.webparsers.domain.Activity.LifeStage;
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

public class Parse_AnnArborObserver implements Parser {

    @Override
    public List<Activity> getEvents(String eventsSourceUrl, String parser, LocationRepository locationRep, ActivityRepository activityRep) {
        List<Activity> eventsOutput = new ArrayList<Activity>();

        /* Initialize States*/
        String defaultZipCode = "48104";
        String defaultCity = "Ann Arbor";
        String defaultState = "Michigan";
        String defaultAddress = "20900 Oakwood Boulevard, Dearborn, MI ";

        String monthRegexRule = "(January|Jan|February|Feb|March|Mar|April|Apr|May|June|July|August|Aug|September|October|Oct|November|Nov|December)";
        //String weekDayRegexRule = "(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)";
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

        //
        Elements events = doc.select(".arborweb_h2");
        for (Element eachEvent : events) {
            int errorCode = 0;
            String eventAddress = defaultAddress;
            // skip title of events
            if ("".equals(eachEvent.text())) {
                continue;
            }
            String eventTitle = eachEvent.text();

            // skip duplicate events
            if (activityRep.isExisted(eventTitle, parser)) {
                continue;
            }
            // can't find any decription
            if (eachEvent.nextElementSibling() == null) {
                errorCode += ErrorCode.NoDescription.getValue();
            }
            String eventDescription = "";
            Element currentDescription = eachEvent.nextElementSibling();
            while (eachEvent.nextElementSibling() != null) {
                // reach the end of event description
                if (currentDescription == null || currentDescription.nextElementSibling() == null || "hr".equals(currentDescription.tagName()))
                    break;

                // fetch each description of event
                String eachDescription = currentDescription.text();
                eventDescription += eachDescription;
                // fetch address
                if (eachDescription.contains("[map]"))
                    eventAddress = eachDescription.replace("[map]", "");

                // go to next description
                currentDescription = currentDescription.nextElementSibling();
            }


            // get date information from description
            String eventDateStr = eventDescription.replaceAll("\\s", "").replace("Sept.", "September.");

            /**
             * start to parse from time
             */
            String fromTimeFormat = "";
            Date eventFromDate = null;

            //fetch from time format 9a.m.-
            String eventFromTime = getRegexString("\\d{1,2}(a|p)\\.m\\.-", "-", eventDateStr).replaceAll("\\.", "");
            if (!"".equals(eventFromTime)) {
                fromTimeFormat = "hhaaa";
            }
            /* fetch from time format 5-9:30p.m*/
            if ("".equals(fromTimeFormat)) {
                eventFromTime = this.getRegexString("\\d{1,2}-\\d{1,2}:\\d{1,2}(a|p)\\.m", "", eventDateStr);
                if (!"".equals(eventFromTime)) {
                    String period = this.getRegexString("(a|p)\\.m", ".", eventFromTime);
                    eventFromTime = this.getRegexString("\\d{1,2}-", "-", eventFromTime) + period;
                    if (!"".equals(eventFromTime)) {
                        fromTimeFormat = "hhaaa";
                    }
                }
            }

            /* fetch from time format 6-9p.m*/
            if ("".equals(fromTimeFormat)) {
                eventFromTime = this.getRegexString("\\d{1,2}-\\d{1,2}(a|p)\\.m", "", eventDateStr);
                if (!"".equals(eventFromTime)) {
                    String period = this.getRegexString("(a|p)\\.m", ".", eventFromTime);
                    eventFromTime = this.getRegexString("\\d{1,2}-", "-", eventFromTime) + period;
                    if (!"".equals(eventFromTime)) {
                        fromTimeFormat = "hhaaa";
                    }
                }
            }


            // set default from time if we can't fetch from time.
            if ("".equals(fromTimeFormat)) {
                eventFromTime = defaultFromTime;
                fromTimeFormat = "hh:mm aaa";
                errorCode += ErrorCode.NoFromTime.getValue();
            }

            /**
             * start to parse from date
             */
            String dateFormat = "";
            String eventFromDateStr = "";

            // parse from date  format: 2016 dates Feb.19
            if ("".equals(dateFormat)) {
                eventFromDateStr = this.getRegexString("\\d{4}dates" + monthRegexRule + "\\.\\d{1,2}", "", eventDateStr);
                if (!"".equals(eventFromDateStr)) {
                    // fetch year
                    String eventFromYear = this.getRegexString("\\d{4}", "", eventFromDateStr);
                    /* fetch from date*/
                    eventFromDateStr = this.getRegexString(monthRegexRule + "\\.\\d{1,2}", "", eventFromDateStr) + "," + eventFromYear;
                    dateFormat = "MMMM.dd,yyyy";
                }
            }
            // parse from date  format: 2016 dates Feb19
            if ("".equals(dateFormat)) {
                eventFromDateStr = this.getRegexString("\\d{4}dates" + monthRegexRule + "\\d{1,2}", "", eventDateStr);
                if (!"".equals(eventFromDateStr)) {
                    // fetch year
                    String eventFromYear = this.getRegexString("\\d{4}", "", eventFromDateStr);
                    /* fetch from date*/
                    eventFromDateStr = this.getRegexString(monthRegexRule + "\\d{1,2}", "", eventFromDateStr) + "," + eventFromYear;
                    dateFormat = "MMMMdd,yyyy";
                }
            }

            // parse from date  format: Mar.15&20,2016
            if ("".equals(dateFormat)) {
                eventFromDateStr = this.getRegexString(monthRegexRule + "\\.\\d{1,2}&\\d{1,2},\\d{4}", "", eventDateStr);
                if (!"".equals(eventFromDateStr)) {
                    // fetch year
                    String eventFromYear = this.getRegexString("\\d{4}", "", eventFromDateStr);
                    /* fetch from date*/
                    eventFromDateStr = this.getRegexString(monthRegexRule + "\\.\\d{1,2}", "", eventFromDateStr) + "," + eventFromYear;
                    dateFormat = "MMMM.dd,yyyy";
                }
            }

            // parse from date  format: Mar.15-20,2016
            if ("".equals(dateFormat)) {
                eventFromDateStr = this.getRegexString(monthRegexRule + "\\.\\d{1,2}-\\d{1,2},\\d{4}", "", eventDateStr);
                if (!"".equals(eventFromDateStr)) {
                    // fetch year
                    String eventFromYear = this.getRegexString("\\d{4}", "", eventFromDateStr);
                    /* fetch from date*/
                    eventFromDateStr = this.getRegexString(monthRegexRule + "\\.\\d{1,2}", "", eventFromDateStr) + "," + eventFromYear;
                    dateFormat = "MMMM.dd,yyyy";
                }
            }

            // parse from date  format: July21-24,2016
            if ("".equals(dateFormat)) {
                eventFromDateStr = this.getRegexString(monthRegexRule + "\\d{1,2}-\\d{1,2},\\d{4}", "", eventDateStr);
                if (!"".equals(eventFromDateStr)) {
                    // fetch year
                    String eventFromYear = this.getRegexString("\\d{4}", "", eventFromDateStr);
                    /* fetch from date*/
                    eventFromDateStr = this.getRegexString(monthRegexRule + "\\d{1,2}", "", eventFromDateStr) + "," + eventFromYear;
                    dateFormat = "MMMMdd,yyyy";
                }
            }

            // parse from date format: November25,2016
            if ("".equals(dateFormat)) {
                eventFromDateStr = this.getRegexString(monthRegexRule + "\\d{1,2}\\,\\d{4}", "", eventDateStr);
                if (!"".equals(eventFromDateStr)) {
                    dateFormat = "MMMMdd,yyyy";
                }
            }

            // parse from date  format: November.25,2016
            if ("".equals(dateFormat)) {
                eventFromDateStr = this.getRegexString(monthRegexRule + "\\.\\d{1,2},\\d{4}", "", eventDateStr);
                if (!"".equals(eventFromDateStr)) {
                    dateFormat = "MMMM.dd,yyyy";
                }
            }

            // parse from date
            if (!"".equals(eventFromDateStr) && eventFromDate == null) {
                SimpleDateFormat formatter = new SimpleDateFormat(dateFormat + " " + fromTimeFormat);
                try {
                    eventFromDate = formatter.parse(eventFromDateStr + " " + eventFromTime);
                } catch (ParseException e) {
                    errorCode += ErrorCode.NoFromDate.getValue();
                    e.printStackTrace();
                }
            }

            if (eventFromDate == null)
                errorCode += ErrorCode.NoFromDate.getValue();

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
            newEvent.setUrl(eventsSourceUrl);
            newEvent.setType(category);
            newEvent.setAddress(eventAddress);
            newEvent.setFromDate(eventFromDate);
            newEvent.setFromTime(eventFromTime);
            newEvent.setCustomData(eventTitle);
            newEvent.setStage(LifeStage.Approved);
            newEvent.setErrorCode(errorCode);
            PostProcessing(newEvent, locationRep);

            eventsOutput.add(newEvent);
        }

        return eventsOutput;
    }

    /**
     * get next date of this year
     *
     * @param date Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday
     * @return
     */
    private Calendar getNextDateOfWeek(String date) {
        Calendar cal = Calendar.getInstance();
        int thisYear = cal.get(Calendar.YEAR);
        date = date.toLowerCase();
        switch (date) {
            case "monday":
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            case "tuesday":
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.TUESDAY && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            case "wednesday":
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.WEDNESDAY && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            case "thursday":
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.THURSDAY && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            case "friday":
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            case "saturday":
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            case "sunday":
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            default:
                cal = null;
                break;

        }

        return cal;
    }

    /**
     * get next date of this year
     *
     * @param date Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday
     * @return
     */
    private Calendar getNextDateOfWeek(String date, int weekOfMonth) {
        Calendar cal = Calendar.getInstance();
        int thisYear = cal.get(Calendar.YEAR);
        date = date.toLowerCase();
        switch (date) {
            case "monday":
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY && cal.get(Calendar.WEEK_OF_MONTH) != weekOfMonth) {
                    break;
                }
                while ((cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY || cal.get(Calendar.WEEK_OF_MONTH) != weekOfMonth) && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            case "tuesday":
                while ((cal.get(Calendar.DAY_OF_WEEK) != Calendar.TUESDAY || cal.get(Calendar.WEEK_OF_MONTH) != weekOfMonth) && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            case "wednesday":
                while ((cal.get(Calendar.DAY_OF_WEEK) != Calendar.WEDNESDAY || cal.get(Calendar.WEEK_OF_MONTH) != weekOfMonth) && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            case "thursday":
                while ((cal.get(Calendar.DAY_OF_WEEK) != Calendar.THURSDAY || cal.get(Calendar.WEEK_OF_MONTH) != weekOfMonth) && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            case "friday":
                while ((cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY || cal.get(Calendar.WEEK_OF_MONTH) != weekOfMonth) && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            case "saturday":
                while ((cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY || cal.get(Calendar.WEEK_OF_MONTH) != weekOfMonth) && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            case "sunday":
                while ((cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY || cal.get(Calendar.WEEK_OF_MONTH) != weekOfMonth) && cal.get(Calendar.YEAR) == thisYear) {
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;

            default:
                cal = null;
                break;

        }

        return cal;
    }
}