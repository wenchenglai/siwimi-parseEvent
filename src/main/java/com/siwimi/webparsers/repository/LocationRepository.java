package com.siwimi.webparsers.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.siwimi.webparsers.domain.Location;

public interface LocationRepository extends MongoRepository<Location, String>, LocationRepositoryCustom{

}
