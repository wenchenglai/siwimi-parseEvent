package com.siwimi.webapi.repository.mongo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.siwimi.webapi.domain.Activity;
import com.siwimi.webapi.repository.ActivityRepositoryCustom;

@Repository
public class ActivityRepositoryImpl implements ActivityRepositoryCustom {
	
	@Autowired
	private MongoTemplate mongoTemplate;

	@SuppressWarnings("static-access")
	@Override
	public Activity queryExistedActivity(String creatorId) {			
		List<Criteria> criterias = new ArrayList<Criteria>();
		
		criterias.add(new Criteria().where("isDeletedRecord").is(false));

		if (creatorId != null) {
			criterias.add(new Criteria().where("creator").is(creatorId));
		}
		
		Criteria c = new Criteria().andOperator(criterias.toArray(new Criteria[criterias.size()]));
		
		return mongoTemplate.findOne(new Query(c), Activity.class, "Activity");
				
	}
		
	@Override
	public Activity saveActivity(Activity newActivity) {		
		mongoTemplate.indexOps(Activity.class).ensureIndex(new GeospatialIndex("location"));
		mongoTemplate.save(newActivity, "Activity");
		return newActivity;
	}	
	
}
