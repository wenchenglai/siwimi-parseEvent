package com.siwimi.webparsers.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.siwimi.webparsers.domain.ActivitySite;

@Repository
public interface ActivitySiteRepository extends MongoRepository<ActivitySite, String>{

}
