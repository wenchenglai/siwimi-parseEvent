 package com.siwimi.webparsers.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.siwimi.webparsers.domain.Activity;

public interface ActivityRepository extends MongoRepository<Activity, String>, ActivityRepositoryCustom{
	//Activity findByIdAndIsDeletedRecordIsFalse(String id);
}
