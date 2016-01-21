package com.siwimi.webparsers.repository;

import com.siwimi.webparsers.domain.Activity;

public interface ActivityRepositoryCustom {
	Activity queryExistedActivity(String creatorId, String title, String description);
	Boolean isExisted(String customData, String parser);
	Activity saveActivity(Activity newActivity);
}
