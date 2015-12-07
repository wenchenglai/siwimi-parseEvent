package com.siwimi.webapi.repository.mongo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.siwimi.webapi.domain.Location;
import com.siwimi.webapi.repository.LocationRepositoryCustom;

@Repository
public class LocationRepositoryImpl implements LocationRepositoryCustom {
	
	@Autowired
	private MongoTemplate mongoTemplate;

	@SuppressWarnings("static-access")
	@Override
	public Location queryLocation(String zipCode, String city, String state) {
		
		List<Criteria> criterias = new ArrayList<Criteria>();
		
 		if (zipCode != null) {			
			// zipCode's type is integer in the database
			criterias.add(new Criteria().where("zipCode").is(Integer.parseInt(zipCode)));
		} else {
			if ((city != null) && (state !=null)) {
				criterias.add(new Criteria().andOperator(new Criteria().where("township").regex(city.trim(), "i"),
						                                 new Criteria().orOperator(Criteria.where("state").regex(state.trim(), "i"),
	                                                                               Criteria.where("stateCode").regex(state.trim(), "i"))));
			}
		}
 		
 		if (criterias.contains(null) || criterias.isEmpty()) {
 			return null;
 		} else { 	 		
 			Criteria c = new Criteria().orOperator(criterias.toArray(new Criteria[criterias.size()]));
 			return mongoTemplate.findOne(new Query(c), Location.class, "Location"); 			
 		}
	}
	
	
	@SuppressWarnings("static-access")
	@Override
	public List<Location> queryFuzzyLocations(String queryText) {
		
		List<Criteria> criterias = new ArrayList<Criteria>();
		
		if (queryText != null) {
			queryText = queryText.trim();
			if (queryText.contains(",")) {
				String[] parts = queryText.split(",");
				String city = parts[0].trim();
				String state = parts[1].trim();
				criterias.add(new Criteria().andOperator(new Criteria().where("township").regex("^"+city, "i"),
                                                         new Criteria().orOperator(Criteria.where("state").regex(state, "i"),
                                                                                   Criteria.where("stateCode").regex(state, "i"))));
			} else if (queryText.matches("^-?\\d+$")) {
				criterias.add(new Criteria().where("zipCode").is(Integer.parseInt(queryText)));
			} else {
				criterias.add(new Criteria().where("township").regex("^"+queryText, "i"));				
			}			
			Criteria c = new Criteria().andOperator(criterias.toArray(new Criteria[criterias.size()]));
			Query q = new Query(c).with(new Sort(Sort.DEFAULT_DIRECTION.ASC,"township"));
			return mongoTemplate.find(q, Location.class, "Location");
		} else
			return null;
	}
}
