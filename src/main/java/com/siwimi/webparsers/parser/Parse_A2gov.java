package com.siwimi.webparsers.parser;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.domain.Activity.LifeStage;
import com.siwimi.webparsers.repository.ActivityRepository;
import com.siwimi.webparsers.repository.LocationRepository;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Parse_A2gov implements Parser {

    @Override
    public List<Activity> getEvents(String eventsSourceUrl, String parser, LocationRepository locationRep, ActivityRepository activityRep) {
        List<Activity> eventsOutput = new ArrayList<Activity>();

        /* Initialize States*/
        String defaultEventUrlForHuman = "http://www.a2gov.org/departments/Parks-Recreation/Pages/events.aspx";
        String defaultAddressAnnArborFarmerMarket = "315 Detroit St";
        String defaultZipCode = "48104";

        String httpResponse = "";
        try {
            // event source url http://www.a2gov.org/departments/Parks-Recreation/Pages/events.aspx
            // this AJAX request to fetch event data
            String eventUrl = eventsSourceUrl;
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

            String eventTitle = eachPanel.select("h4 a").text();
            String address = "";
            
			if (eventTitle != null) {
				if (eventTitle.toLowerCase().contains("ann arbor farmers market")) {
					address = defaultAddressAnnArborFarmerMarket;
				}
			} 
            
            
            String eventTime = eachPanel.select("p strong").text();
            String eventDescription = eachPanel.select("p").first().textNodes().toString();

            String eventId = eachPanel.select(".panel-collapse").first().id();

			if (activityRep.isExisted(eventTitle + eventId, parser)) {
				continue;
			}

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
            Activity newEvent = new Activity();
			newEvent.setIsDeletedRecord(false);
            newEvent.setCreatedDate(new Date());
            newEvent.setParser(parser);
            newEvent.setTitle(eventTitle);
            newEvent.setDescription(eventDescription);
            newEvent.setUrl(defaultEventUrlForHuman);
            newEvent.setZipCode(defaultZipCode);
            newEvent.setAddress(address);
            newEvent.setType(defaultCategory);
            newEvent.setFromDate(eventFromDate);
            newEvent.setFromTime(fromTime);
            newEvent.setToDate(eventToDate);
            newEvent.setToTime(toTime);
            newEvent.setErrorCode(errorCode);
            newEvent.setCustomData(eventTitle + eventId);
            newEvent.setStage(LifeStage.Approved);


            // update location and time zone
            PostProcessing(newEvent, locationRep);
            eventsOutput.add(newEvent);
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
}