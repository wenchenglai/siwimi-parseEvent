package com.siwimi.webapi.repository;

import java.util.List;

import com.siwimi.webapi.domain.Location;

public interface LocationRepositoryCustom {
		Location queryLocation(String zipCode, String city, String state);
		List<Location> queryFuzzyLocations(String queryText);	
}