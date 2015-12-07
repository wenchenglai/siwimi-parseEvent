 package com.siwimi.webapi.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.siwimi.webapi.domain.Activity;

public interface ActivityRepository extends MongoRepository<Activity, String>, ActivityRepositoryCustom{
	//Activity findByIdAndIsDeletedRecordIsFalse(String id);
}
