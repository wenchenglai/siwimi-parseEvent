package com.siwimi.webparsers.parser;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.domain.Location;
import com.siwimi.webparsers.repository.ActivityRepository;
import com.siwimi.webparsers.repository.LocationRepository;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parse_A2gov implements ParseWebsite {

    @Override
    public List<Activity> getEvents(String eventsSourceUrl, String parser, LocationRepository locationRep) {
        List<Activity> eventsOutput = new ArrayList<Activity>();

        /* Initialize States*/
        String defaultZipCode = "48104";
        String defaultEventHostUrl = "http://www.a2gov.org/departments/Parks-Recreation/Pages/events.aspx";
        String defaultCity = "Ann Arbor";
        String defaultState = "Michigan";
        String defaultCategory = "misc";

        String httpResponse = "";
        try {
            String eventUrl = "https://calendar.a2gov.org/EventListSyndicator.aspx?type=N&number=5&location=3-0-0&adpid=6&nem=No+events+are+available+that+match+your+request&sortorder=ASC&ver=2.0&target=";
            httpResponse = this.getHttpResponse(eventUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // fail to get any event data
        if ("".equals(httpResponse))
            return eventsOutput;


        // remove javascript
        String responseHtml = httpResponse.trim().replace("var response='success'; jQuery('#').html(\"", "").replace("\")", "");

        // decode html
        responseHtml = responseHtml.replace("\\u003e", ">");
        responseHtml = responseHtml.replace("\\u003c", "<");
        responseHtml = responseHtml.replace("&nbsp;", " ");

        // unescape "
        responseHtml = responseHtml.replace("\\\"", "\"");

        // parse html
        Document doc = null;
        try {
            doc = Jsoup.parse(responseHtml);
        } catch (Exception e){
            doc = null;
            e.printStackTrace();
        }
        if (doc == null)
            return eventsOutput;


        // select event panel
        Elements panelElements = doc.select(".panel");

        // fetch event data
        for (Element eachPanel : panelElements) {
            int errorCode = 0;
            Activity event = new Activity();

            String eventTitle = eachPanel.select("h4 a").text();
            String eventTime = eachPanel.select("p strong").text();
            String eventDescription = eachPanel.select("p").first().textNodes().toString();

            String eventId = eachPanel.select(".panel-collapse").first().id();


            Date eventFromDate = null;
            Date eventToDate = null;

            // get date of event
            String datePattern = "(?i)[a-zA-Z]+\\s\\d{0,2},\\s\\d{4}";
            String eventDate = this.getRegexString(datePattern, "", eventTime);
            if ("".equals(eventDate))
                errorCode += ErrorCode.NoFromDate.getValue();

            // get when event start
            String fromTimePattern = "(?i)@\\s\\d{1,2}:\\d{1,2}\\s(AM|PM)\\s";
            String fromTime = this.getRegexString(fromTimePattern, "@", eventTime);
            if ("".equals(fromTime)) {
                fromTime = defaultFromTime;
                errorCode += ErrorCode.NoFromTime.getValue();
            }

            // get when event end
            String toTimePattern = "(?i)-\\s\\d{1,2}:\\d{1,2}\\s(AM|PM)";
            String toTime = this.getRegexString(toTimePattern, "-", eventTime);
            if ("".equals(toTime)) {
                toTime = defaultFromTime;
            }

            // format event from date and end date
            SimpleDateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy hh:mm aaa");
            if (!"".equals(eventDate) && !"".equals(fromTime)){
                try {
                    eventFromDate = formatter.parse(eventDate.trim() + " " + fromTime);
                } catch (Exception e){
                    errorCode += ErrorCode.NoFromDate.getValue();
                }
            }

            if (!"".equals(eventDate) && !"".equals(toTime)){
                try {
                    eventToDate = formatter.parse(eventDate.trim() + " " + toTime);
                } catch (Exception e){
                }
            }


            // store event data
            event.setParser(parser);
            event.setCreatedDate(new Date());
            event.setTitle(eventTitle);
            event.setDescription(eventDescription);
            event.setUrl(defaultEventHostUrl);
            event.setCity(defaultCity);
            event.setState(defaultState);
            event.setZipCode(defaultZipCode);
            event.setType(defaultCategory);
            event.setFromDate(eventFromDate);
            event.setFromTime(fromTime);
            event.setToDate(eventToDate);
            event.setToTime(toTime);
            event.setErrorCode(errorCode);

            // update location and timezone
            PostProcessing(event, locationRep);
            eventsOutput.add(event);
        }


        return eventsOutput;
    }




    /**
     * send http request and get response
     * @param targetUrl
     * @return response
     * @throws Exception
     */
    public String getHttpResponse(String targetUrl) throws Exception {
        URL url = new URL(targetUrl);
        BufferedReader Buffer = new BufferedReader(new InputStreamReader(url.openStream()));
        String response = "";
        String strTemp = "";
        while (null != (strTemp = Buffer.readLine())) {
            response += strTemp;
        }
        return response;
    }


    private void PostProcessing(Activity newEvent, LocationRepository locationRep) {
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
    private String getRegexString(String pattern,String filter ,String text){
        String result = "";

        Pattern rulePatten = Pattern.compile(pattern);
        Matcher patternMatcher = rulePatten.matcher(text);
        if (patternMatcher.find()) {
            result = patternMatcher.group(0).replace(filter,"").trim();
        }
        return result;
    }
}