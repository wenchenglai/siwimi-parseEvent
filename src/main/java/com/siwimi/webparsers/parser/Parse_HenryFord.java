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

public class Parse_HenryFord implements Parser {

    @Override
    public List<Activity> getEvents(String eventsSourceUrl, String parser, LocationRepository locationRep, ActivityRepository activityRep) {
        List<Activity> eventsOutput = new ArrayList<Activity>();

        /* Initialize States*/
        String defaultZipCode = "48124";
        String defaultCity = "Dearborn";
        String defaultState = "Michigan";
        String defaultAddress = "20900 Oakwood Boulevard, Dearborn, MI ";

        String monthRegexRule = "(January|February|March|April|May|June|July|August|September|October|November|December)";
        String weekDayRegexRule = "(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)";
        Category category = Category.museum;


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

        String eventTableId = "ctl00_ContentPlaceHolder1_ctl05_dl_list";
        int eventCounter = 1;

        Elements events = doc.select("#" + eventTableId + " table tr");
        for (Element eachEvent : events) {
            int errorCode = 0;

            /**
             * this is for same event with multiple date ex every Sunday of each month
             * then we can duplicate those event by date
             */
            List<Date> eventFollowingFromDates = new ArrayList<Date>();


            // skip empty tr
            if ("".equals(eachEvent.text())) {
                continue;
            }

            // generate event Id
            String eventCounterStr = String.format("%02d", eventCounter);

            // fetch event data
            Elements eventTitleElement = eachEvent.select("#" + eventTableId + "_ctl" + eventCounterStr + "_lb_event");
            String eventTitle = eventTitleElement.text().trim();
            if ("".equals(eventTitle)) {
                errorCode += ErrorCode.NoTitle.getValue();
            }
            String eventUrl = eventTitleElement.attr("abs:href");
            //eventUrl
            String eventDescription = eachEvent.select("#" + eventTableId + "_ctl" + eventCounterStr + "_Label3").text().trim();
            if ("".equals(eventDescription)) {
                errorCode += ErrorCode.NoDescription.getValue();
            }

            Date eventFromDate = null;
            String fromTimeFormat = "";
            String eventDate = eachEvent.select("#" + eventTableId + "_ctl" + eventCounterStr + "_Label2").text().trim().replace(" ", "");


            /**
             * start to parse from time
             */
            // fetch from time format 11:00am-
            String eventFromTime = getRegexString("\\d{1,2}:\\d{1,2}?(am|pm)(to|-|–){1}", "", eventDate).replaceAll("(-|to|–)", "");
            if (!"".equals(eventFromTime)) {
                fromTimeFormat = "hh:mmaaa";
            }

            // fetch from time format 11am-
            if ("".equals(fromTimeFormat)) {
                eventFromTime = getRegexString("\\d{1,2}(am|pm)(to|-|–){1}", "", eventDate).replaceAll("(-|to|–)", "");
                if (!"".equals(eventFromTime)) {
                    fromTimeFormat = "hhaaa";
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
            // parse from date format: November25,2016
            String eventFromDateStr = getRegexString("^" + monthRegexRule + "\\d{1,2}\\,\\d{4}", "", eventDate);
            if (!"".equals(eventFromDateStr)) {
                dateFormat = "MMMMdd,yyyy";
            }

            // parse from date  format: November25
            if ("".equals(dateFormat)) {
                eventFromDateStr = getRegexString("^" + monthRegexRule + "\\d{1,2}", "", eventDate);
                String evnetFromDateYearStr = getRegexString("\\d{4}", "", eventDate);
                if (!"".equals(eventFromDateStr) && !"".equals(evnetFromDateYearStr)) {
                    dateFormat = "MMMMdd,yyyy";
                    eventFromDateStr += "," + evnetFromDateYearStr;
                }
            }
            // parse from date  Every Saturday, every month
            if ("".equals(dateFormat)) {
                String everyDateStr = getRegexString("Every" + weekDayRegexRule + ",everymonth", ",", eventDate).replaceAll("(Every|everymonth)", "");
                Calendar nextDateCal = this.getNextDateOfWeek(everyDateStr);

                if (nextDateCal != null) {
                    eventFromDate = nextDateCal.getTime();
                    int thisYear = nextDateCal.get(Calendar.YEAR);

                    //fetch following event date of this year
                    while (nextDateCal.get(Calendar.YEAR) == thisYear) {
                        nextDateCal.add(Calendar.DAY_OF_WEEK, 7);
                        if (nextDateCal.get(Calendar.YEAR) == thisYear)
                            eventFollowingFromDates.add(nextDateCal.getTime());

                    }
                }
            }
            // parse from date Second Monday of each Month
            if ("".equals(dateFormat) && eventFromDate == null) {
                String everyDateStr = getRegexString("Second" + weekDayRegexRule + "ofeachMonth", "", eventDate).replaceAll("(First|Second|Third|Fourth|ofeachMonth)", "");

                Calendar nextDateCal = this.getNextDateOfWeek(everyDateStr, 2);

                if (nextDateCal != null) {
                    eventFromDate = nextDateCal.getTime();
                    int thisYear = nextDateCal.get(Calendar.YEAR);

                    //fetch following event date of this year
                    while (nextDateCal.get(Calendar.YEAR) == thisYear) {
                        nextDateCal.add(Calendar.DAY_OF_WEEK, 1);

                        if (nextDateCal.get(Calendar.WEEK_OF_MONTH) == 2) {
                            int whichDate = 0;
                            switch (everyDateStr.toLowerCase()) {
                                case "monday":
                                    whichDate = Calendar.MONDAY;
                                    break;

                                case "tuesday":
                                    whichDate = Calendar.TUESDAY;
                                    break;

                                case "wednesday":
                                    whichDate = Calendar.WEDNESDAY;
                                    break;

                                case "thursday":
                                    whichDate = Calendar.THURSDAY;
                                    break;

                                case "friday":
                                    whichDate = Calendar.FRIDAY;
                                    break;

                                case "saturday":
                                    whichDate = Calendar.SATURDAY;
                                    break;

                                case "sunday":
                                    whichDate = Calendar.SUNDAY;
                                    break;
                            }

                            if (nextDateCal.get(Calendar.DAY_OF_WEEK) == whichDate) {
                                eventFollowingFromDates.add(nextDateCal.getTime());
                            }
                        }
                    }
                }
            }


            if (!"".equals(eventFromDateStr) && eventFromDate == null) {
                SimpleDateFormat formatter = new SimpleDateFormat(dateFormat + fromTimeFormat);
                try {
                    eventFromDate = formatter.parse(eventFromDateStr + eventFromTime);
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
            newEvent.setUrl(eventUrl);
            newEvent.setType(category);
            newEvent.setAddress(defaultAddress);

            // fetch image
            String imageUrl = eachEvent.select("td").first().select("img").attr("abs:src");
            errorCode += this.setImage(newEvent, imageUrl);

            newEvent.setFromDate(eventFromDate);
            newEvent.setFromTime(eventFromTime);
            newEvent.setCustomData(eventUrl);
            newEvent.setStage(LifeStage.Approved);

            newEvent.setErrorCode(errorCode);
            PostProcessing(newEvent, locationRep);

            eventsOutput.add(newEvent);
            eventCounter++;
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