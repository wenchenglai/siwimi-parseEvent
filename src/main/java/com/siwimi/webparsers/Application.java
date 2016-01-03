package com.siwimi.webparsers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.domain.ActivitySite;
import com.siwimi.webparsers.parser.ParseWebsite;
import com.siwimi.webparsers.repository.ActivityRepository;
import com.siwimi.webparsers.repository.ActivitySiteRepository;
import com.siwimi.webparsers.repository.LocationRepository;

@SpringBootApplication
public class Application implements CommandLineRunner {
	class ParseSummary {
		public int TotalParsedEvent;
		public int TotalSavedEvent;
		
		ParseSummary(int totalParsedEvent, int totalSavedEvent) {
			this.TotalParsedEvent = totalParsedEvent;
			this.TotalSavedEvent = totalSavedEvent;
		}
	};
	
	@Autowired
	private ActivityRepository activityRep;
	
	@Autowired
	private LocationRepository locationRep;
	
	@Autowired
	private ActivitySiteRepository activitySiteRepository;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		List<ActivitySite> sites = activitySiteRepository.findAll();		
		if (sites == null) {
			System.out.println("There are no sites to parse.  Please make sure there are data at EventParserSite collection.");
			return;
		}
		
		Map<String, ParseSummary> map = new HashMap<String, ParseSummary>();
		for (ActivitySite site : sites) { 
			try {
				if (site.getIsActive() == null || !site.getIsActive())
                    continue;
				
				String classPath = "com.siwimi.webparsers.parser." + site.getClassName();
				// NOTE: rename parse to parser
				ParseWebsite parser = (ParseWebsite) ParseWebsite.class.getClassLoader().loadClass(classPath).newInstance();
				List<Activity> events = parser.getEvents(site.getUrl(), site.getSiteName(), locationRep);
				int totalEventParsed = events.size();
				int totalEventSaved = parser.saveEvents(events, activityRep);
				
				ParseSummary summary = new ParseSummary(totalEventParsed, totalEventSaved);
				String key = String.format("%1s: %2s", site.getClassName(), site.getSiteName());
				map.put(key, summary);			
			} catch(Exception e) {	
				String errorMsg = "Siwimi Exception: Caught at the global catcher, please identify the problem for this parser:%1s";
				System.out.println(String.format(errorMsg, site.getClassName()));
				e.printStackTrace();
			}
		}
		System.out.println("Finished all parsers operations.");
		printSummary(map);
	}
	
	private void printSummary(Map<String, ParseSummary> map) {
		System.out.println("******* Now printing summary ********");
		for (Map.Entry<String, ParseSummary> entry : map.entrySet())
		{
			ParseSummary summary = entry.getValue();
			String msg = String.format("%1s has parsed %2s, added %3s events.", entry.getKey(), summary.TotalParsedEvent, summary.TotalSavedEvent);
		    System.out.println(msg);
		}
		System.out.println("******* End of parser summary ********");
	}
}
