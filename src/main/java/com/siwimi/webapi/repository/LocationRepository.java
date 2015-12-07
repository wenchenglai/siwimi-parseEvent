package com.siwimi.webapi.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.siwimi.webapi.domain.Location;

public interface LocationRepository extends MongoRepository<Location, String>, LocationRepositoryCustom{

}
