package com.siwimi.webapi.parser;

import java.util.List;

import com.siwimi.webapi.domain.Activity;
import com.siwimi.webapi.repository.ActivityRepository;
import com.siwimi.webapi.repository.LocationRepository;

public interface ParseWebsite {
	List<Activity> retrieve(String url);
	void saveActivity(List<Activity> activities, ActivityRepository activityRep, LocationRepository locationRep);
}
