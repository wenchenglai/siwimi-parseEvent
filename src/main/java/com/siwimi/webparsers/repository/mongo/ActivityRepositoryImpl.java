package com.siwimi.webparsers.repository.mongo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.repository.ActivityRepositoryCustom;

@Repository
public class ActivityRepositoryImpl implements ActivityRepositoryCustom {
	
	@Autowired
	private MongoTemplate mongoTemplate;

	@SuppressWarnings("static-access")
	@Override
	public Activity queryExistedActivity(String creatorId, String title, String description) {			
		List<Criteria> criterias = new ArrayList<Criteria>();
		
		criterias.add(new Criteria().where("isDeletedRecord").is(false));

		if (creatorId != null) {
			criterias.add(new Criteria().where("creator").is(creatorId));
		}
		
		if (title != null) {
			criterias.add(new Criteria().where("title").is(title));
		}
		
		if (description != null) {
			criterias.add(new Criteria().where("description").is(description));
		}
		
		Criteria c = new Criteria().andOperator(criterias.toArray(new Criteria[criterias.size()]));
		
		return mongoTemplate.findOne(new Query(c), Activity.class, "Activity");
				
	}
	
	@SuppressWarnings("static-access")
	@Override
	public Boolean isExisted(String customData) {
		if (customData == null) {
			return false;
		}
		
		List<Criteria> criterias = new ArrayList<Criteria>();
		
		criterias.add(new Criteria().where("isDeletedRecord").is(false));
		criterias.add(new Criteria().where("customData").is(customData));
		
		Criteria c = new Criteria().andOperator(criterias.toArray(new Criteria[criterias.size()]));
		
		Activity one = mongoTemplate.findOne(new Query(c), Activity.class, "Activity");
		
		return one != null;
	}	
		
	@Override
	public Activity saveActivity(Activity newActivity) {		
		mongoTemplate.indexOps(Activity.class).ensureIndex(new GeospatialIndex("location"));
		mongoTemplate.save(newActivity, "Activity");
		return newActivity;
	}	
	
}
