package com.siwimi.webparsers.repository;

import com.siwimi.webparsers.domain.Activity;

public interface ActivityRepositoryCustom {
	Activity queryExistedActivity(String creatorId);
	Activity saveActivity(Activity newActivity);
}
