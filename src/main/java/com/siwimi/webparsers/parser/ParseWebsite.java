package com.siwimi.webparsers.parser;

import java.util.List;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.repository.ActivityRepository;
import com.siwimi.webparsers.repository.LocationRepository;

public interface ParseWebsite {
	List<Activity> retrieve(String url);
	void saveActivity(List<Activity> activities, ActivityRepository activityRep, LocationRepository locationRep);
}
