package com.siwimi.webapi.repository;

import com.siwimi.webapi.domain.Activity;

public interface ActivityRepositoryCustom {
	Activity queryExistedActivity(String creatorId);
	Activity saveActivity(Activity newActivity);
}
