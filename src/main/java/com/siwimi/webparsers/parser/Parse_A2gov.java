package com.siwimi.webparsers.parser;


import com.siwimi.webparsers.domain.Activity;
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
    public List<Activity> retrieve(String url) {
        List<Activity> activities = new ArrayList<Activity>();

        try {
            String eventUrl = "https://calendar.a2gov.org/EventListSyndicator.aspx?type=N&number=5&location=3-0-0&adpid=6&nem=No+events+are+available+that+match+your+request&sortorder=ASC&ver=2.0&target=";
            String httpResponse = this.getHttpResponse(eventUrl);

            // remove javascript
            String responseHtml = httpResponse.trim().replace("var response='success'; jQuery('#').html(\"", "").replace("\")", "");

            // decode html
            responseHtml = responseHtml.replace("\\u003e", ">");
            responseHtml = responseHtml.replace("\\u003c", "<");
            responseHtml = responseHtml.replace("&nbsp;", " ");

            // unescape "
            responseHtml = responseHtml.replace("\\\"", "\"");

            // parse html
            Document doc = Jsoup.parse(responseHtml);

            // select event panel
            Elements panelElements = doc.select(".panel");

            // fetch event data
            for (Element eachPanel : panelElements) {
                Activity activity = new Activity();

                String eventTitle = eachPanel.select("h4 a").text();
                String eventTime = eachPanel.select("p strong").text();
                String eventDescription = eachPanel.select("p").first().textNodes().toString();

                String eventId = eachPanel.select(".panel-collapse").first().id();
                // store activity data
                activity.setCreator("Siwimi robot : City of Ann Arbor " + eventId);
                activity.setCreatedDate(new Date());

                activity.setTitle(eventTitle);
                activity.setDescription(eventDescription);
                // fixed url
                activity.setUrl("http://www.a2gov.org/departments/Parks-Recreation/Pages/events.aspx");
                activity.setState("MI");

                // get event from date and end date
                // get event date
                String datePattern = "(?i)[a-zA-Z]+\\s\\d{0,2},\\s\\d{4}";
                Pattern rule = Pattern.compile(datePattern);
                Matcher dateMatcher = rule.matcher(eventTime);
                if (dateMatcher.find()) {
                    String eventDate = dateMatcher.group(0);
                    SimpleDateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy hh:mm aaa");

                    // get when event start
                    String fromTimePattern = "(?i)@\\s\\d{1,2}:\\d{1,2}\\s(AM|PM)\\s";
                    Pattern fromTimeRule = Pattern.compile(fromTimePattern);
                    Matcher fromTimeMatcher = fromTimeRule.matcher(eventTime);
                    if (fromTimeMatcher.find()) {
                        String fromTime = fromTimeMatcher.group(0).replace("@","").trim();
                        Date eventFromDate = formatter.parse(eventDate.trim() + " " + fromTime);
                        //Date eventFromDate = formatter.parse(eventDate.trim());
                        activity.setFromDate(eventFromDate);
                    }
                    // get when event end
                    String toTimePattern = "(?i)-\\s\\d{1,2}:\\d{1,2}\\s(AM|PM)";
                    Pattern toTimeRule = Pattern.compile(toTimePattern);
                    Matcher toTimeMatcher = toTimeRule.matcher(eventTime);
                    if (toTimeMatcher.find()) {
                        String toTime = toTimeMatcher.group(0).replace("-","").trim();
                        //Date EventFromDate = formatter.format(eventDate);
                        Date eventToDate = formatter.parse(eventDate.trim() + " " + toTime);
                        activity.setToDate(eventToDate);
                    }


                }




                activities.add(activity);
            }
        } catch (Exception e) {
        }


        return activities;
    }

    @Override
    public void saveActivity(List<Activity> activities, ActivityRepository activityRep, LocationRepository locationRep) {
        if (activities != null) {
            for (Activity activity : activities) {
                activity = updateTime(activity);
                // only insert new event
                if (activityRep.queryExistedActivity(activity.getCreator()) == null)
                    activityRep.saveActivity(activity);
            }
        }
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


    /**
     * update time
     * @param activity
     * @return
     */
    public Activity updateTime(Activity activity) {
        // set time zone of Michigan
        SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE MMMM dd, yyyy hh:mm aaa");
        String fromDateString = dateFormatter.format(activity.getFromDate());
        String toDateString = dateFormatter.format(activity.getToDate());
        dateFormatter.setTimeZone(TimeZone.getTimeZone("US/Michigan"));

        try {
            activity.setFromDate(dateFormatter.parse(fromDateString));
            activity.setToDate(dateFormatter.parse(toDateString));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return activity;
    }
}