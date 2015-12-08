package com.siwimi.webparsers.repository;

import com.siwimi.webparsers.domain.Location;

public interface LocationRepositoryCustom {
		Location queryLocation(String zipCode, String city, String state);	
}