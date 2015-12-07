package com.siwimi.webapi.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.siwimi.webapi.domain.ActivitySite;

@Repository
public interface ActivitySiteRepository extends MongoRepository<ActivitySite, String>{

}
